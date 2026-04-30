const express = require('express');
const mongoose = require('mongoose');
const path = require('path');
const bodyParser = require('body-parser');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const cors = require('cors');
require('dotenv').config();

const app = express();
app.use(bodyParser.json({ limit: '50mb' }));
app.use(bodyParser.urlencoded({ limit: '50mb', extended: true }));
app.use(express.static(path.join(__dirname, 'public')));
app.use(cors());

// --- MongoDB Connect ---
const dbURI = process.env.MONGODB_URI || "mongodb://localhost:27017/alsmsrecive";
mongoose.connect(dbURI)
    .then(() => console.log('MongoDB connected...'))
    .catch(err => console.log(err));


// --- ১. স্কিমা (Models) ---
const UserSchema = new mongoose.Schema({
    email: { type: String, unique: true, sparse: true },
    phone: { type: String, unique: true, sparse: true },
    password: { type: String, required: true },
    role: { type: String, enum: ['user', 'admin'], default: 'user' },
    isBlocked: { type: Boolean, default: false }, 
    createdAt: { type: Date, default: Date.now },
    planExpiresAt: { type: Date, default: () => Date.now() + 24*60*60*1000 }, // ১ দিন ফ্রি
    deviceLimit: { type: Number, default: 1 },
    telegramBotToken: { type: String, default: "" },
    telegramChatId: { type: String, default: "" },
    activeSessions: [{
        deviceId: String,
        deviceName: String,
        loginTime: { type: Date, default: Date.now }
    }]
});
const User = mongoose.model('User', UserSchema);

const MessageSchema = new mongoose.Schema({
    userId: { type: String, required: true },
    type: String,
    sender: String,
    message: String,
    deviceId: { type: String, default: "unknown" },
    deviceName: { type: String, default: "Unknown Device" },
    timestamp: { type: Date, default: Date.now },
    isDeleted: { type: Boolean, default: false },
    deletedAt: { type: Date }
});
const Message = mongoose.model('Message', MessageSchema);

const CallLogSchema = new mongoose.Schema({
    userId: { type: String, required: true },
    number: String,
    type: String,
    date: Date,
    duration: String,
    syncedAt: { type: Date, default: Date.now }
});
const CallLog = mongoose.model('CallLog', CallLogSchema);

const ArchivedMessageSchema = new mongoose.Schema({
    originalId: String,
    userId: String,
    type: String,
    sender: String,
    message: String,
    timestamp: Date,
    archivedAt: { type: Date, default: Date.now }
});
const ArchivedMessage = mongoose.model('ArchivedMessage', ArchivedMessageSchema);

const BroadcastSchema = new mongoose.Schema({
    title: { type: String, required: true },
    message: { type: String, required: true },
    imageUrl: { type: String, default: "" },
    link: { type: String, default: "" },
    linkText: { type: String, default: "View Details" },
    isActive: { type: Boolean, default: true },
    createdAt: { type: Date, default: Date.now }
});
const Broadcast = mongoose.model('Broadcast', BroadcastSchema);

const OrderSchema = new mongoose.Schema({
    userId: { type: String, required: true },
    planType: { type: Number, required: true }, // e.g. 18, 29, 49, 99
    amountBDT: { type: Number, required: true },
    amountUSDT: { type: Number, required: true },
    deviceLimit: { type: Number, required: true },
    paymentMethod: { type: String, enum: ['bkash', 'nagad', 'binance', 'none'], default: 'none' },
    status: { type: String, enum: ['pending', 'completed', 'expired'], default: 'pending' },
    transactionId: { type: String, default: "" }, // Filled when verified
    createdAt: { type: Date, default: Date.now },
    expiresAt: { type: Date, default: () => Date.now() + 60*60*1000 } // 1 hour expiry
});
const Order = mongoose.model('Order', OrderSchema);

const TransactionSchema = new mongoose.Schema({
    transactionId: { type: String, required: true, unique: true },
    amount: { type: Number, required: true },
    sender: { type: String },
    method: { type: String, enum: ['bkash', 'nagad', 'binance'], required: true },
    isUsed: { type: Boolean, default: false },
    usedByUserId: { type: String, default: "" },
    createdAt: { type: Date, default: Date.now }
});
const Transaction = mongoose.model('Transaction', TransactionSchema);


// --- ২. মিডলওয়্যার (API রুটের আগেই থাকতে হবে) ---

const authMiddleware = async (req, res, next) => {
    const token = req.header('x-auth-token');
    if (!token) return res.status(401).json({ msg: 'No token, authorization denied' });

    try {
        const decoded = jwt.verify(token, process.env.JWT_SECRET || 'your_default_secret');
        const user = await User.findById(decoded.user.id);
        
        if (!user) return res.status(401).json({ msg: 'User not found' });
        if (user.isBlocked) return res.status(403).json({ msg: 'Your account has been blocked.' });

        // প্ল্যান চেক (অ্যাডমিন বাদে)
        if (user.role !== 'admin' && user.planExpiresAt && new Date() > new Date(user.planExpiresAt)) {
            return res.status(402).json({ msg: 'Plan Expired! Please renew subscription.' });
        }

        req.user = user;
        next();
    } catch (err) {
        console.error('[Auth Error]', err.message);
        res.status(401).json({ msg: 'Token is not valid' });
    }
};

const adminAuthMiddleware = (req, res, next) => {
    if (req.user && req.user.role === 'admin') {
        next();
    } else {
        res.status(403).json({ msg: 'Access denied. Admins only.' });
    }
};


// --- ৩. API Routes ---

// === Auto Subscription System ===

// Webhook for receiving SMS from the external app
app.post('/api/webhook/sms', async (req, res) => {
    try {
        // The external app sends structured JSON data directly
        const { secret_key, amount, trx_id, provider, sender } = req.body;

        // Verify Secret Key
        const expectedKey = process.env.WEBHOOK_SECRET || "ALSMS_AUTO_VERIFY_123";
        // Also support the user's legacy gateway secret key if needed, or stick to ours
        if (secret_key !== expectedKey && secret_key !== "YOUR_GATEWAY_SECRET") { // Added fallback if they haven't updated the app's secret key
            return res.status(401).json({ msg: "Invalid secret key" });
        }

        if (!trx_id || !amount) {
            return res.status(400).json({ msg: "trx_id and amount are required" });
        }

        const parsedAmount = parseFloat(amount);
        if (isNaN(parsedAmount) || parsedAmount <= 0) {
            return res.status(400).json({ msg: "Invalid amount" });
        }

        // Detect Method from provider
        let method = 'bkash';
        const lowerProvider = (provider || "").toLowerCase();
        const lowerSender = (sender || "").toLowerCase();
        
        if (lowerProvider.includes('nagad') || lowerSender.includes('nagad')) {
            method = 'nagad';
        } else if (lowerProvider.includes('binance') || lowerSender.includes('binance')) {
            method = 'binance';
        }

        // Check if transaction already exists
        const existingTx = await Transaction.findOne({ transactionId: trx_id });
        if (existingTx) {
            return res.status(200).json({ msg: "Transaction already exists" });
        }

        // Save new transaction
        const newTx = new Transaction({
            transactionId: trx_id,
            amount: parsedAmount,
            sender: sender || "Unknown",
            method: method
        });

        await newTx.save();
        res.status(200).json({ msg: "Transaction successfully saved", data: newTx });

    } catch (err) {
        console.error("Webhook Error:", err);
        res.status(500).json({ msg: "Server error" });
    }
});

// Create Order
app.post('/api/plans/create-order', authMiddleware, async (req, res) => {
    try {
        const { planType, paymentMethod } = req.body;
        
        let amountBDT = 0;
        let amountUSDT = 0;
        let deviceLimit = 1;

        if (planType === 18) { amountBDT = 18; amountUSDT = 0.26; deviceLimit = 1; }
        else if (planType === 29) { amountBDT = 29; amountUSDT = 0.35; deviceLimit = 2; }
        else if (planType === 49) { amountBDT = 49; amountUSDT = 0.52; deviceLimit = 5; }
        else if (planType === 99) { amountBDT = 99; amountUSDT = 0.95; deviceLimit = 21; }
        else {
            return res.status(400).json({ msg: "Invalid plan type" });
        }

        const newOrder = new Order({
            userId: req.user._id,
            planType,
            amountBDT,
            amountUSDT,
            deviceLimit,
            paymentMethod: paymentMethod || 'none'
        });

        const savedOrder = await newOrder.save();
        res.json({ msg: "Order created", orderId: savedOrder._id, amountBDT, amountUSDT });
    } catch (err) {
        console.error(err.message);
        res.status(500).send('Server Error');
    }
});

// For Binance Verification, we import needed modules
const crypto = require('crypto');
const axios = require('axios');

async function verifyBinanceTransaction(orderIdToMatch, expectedAmountUSDT) {
    const BINANCE_API_KEY = process.env.BINANCE_API_KEY || '4PmqYTo2sxRWOze7GSGucVj3xytO2dgAoRfo3bXoIAUHKj9vooLQDnkPjl0ulQ5f';
    const BINANCE_SECRET_KEY = process.env.BINANCE_SECRET_KEY || 'srTePTkgNUhxQPVpeLrTqEC57CQfYoxcOxiTuNLokA1y6Sy4WAMdLW5mf9OuVssA';
    const BASE_URL = "https://api.binance.com";
    
    try {
        const timestamp = Date.now();
        const queryParams = `timestamp=${timestamp}`;
        const signature = crypto.createHmac('sha256', BINANCE_SECRET_KEY).update(queryParams).digest('hex');
        
        const url = `${BASE_URL}/sapi/v1/pay/transactions?${queryParams}&signature=${signature}`;
        
        const response = await axios.get(url, {
            headers: { 'X-MBX-APIKEY': BINANCE_API_KEY }
        });

        if (response.data && response.data.data) {
            const transactions = response.data.data;
            for (let tx of transactions) {
                const amountFloat = parseFloat(tx.amount || '0');
                if (amountFloat > 0 && tx.orderId === orderIdToMatch) {
                    // Check amount. Give a tiny margin of error or just require it to be strictly greater/equal.
                    // Actually, if the user paid at least the required amount, approve.
                    if (amountFloat >= (expectedAmountUSDT - 0.05)) {
                        return true;
                    }
                }
            }
        }
        return false;
    } catch (error) {
        console.error("Binance Verify Error:", error.message);
        return false;
    }
}

// Verify Order
app.post('/api/plans/verify-order', authMiddleware, async (req, res) => {
    try {
        const { orderId, transactionId } = req.body; // transactionId can be Bkash TrxID or Binance OrderID

        const order = await Order.findById(orderId);
        if (!order) return res.status(404).json({ msg: "Order not found" });
        if (order.userId !== req.user._id.toString()) return res.status(403).json({ msg: "Unauthorized order access" });
        if (order.status !== 'pending') return res.status(400).json({ msg: "Order is already " + order.status });
        if (new Date() > new Date(order.expiresAt)) {
            order.status = 'expired';
            await order.save();
            return res.status(400).json({ msg: "Order has expired. Please create a new one." });
        }

        let isVerified = false;

        if (order.paymentMethod === 'binance') {
            isVerified = await verifyBinanceTransaction(transactionId, order.amountUSDT);
            if (!isVerified) {
                return res.status(400).json({ msg: "Binance transaction not found or amount insufficient." });
            }
            order.transactionId = transactionId;
        } else {
            // Local Webhook DB check (Bkash/Nagad)
            const tx = await Transaction.findOne({ transactionId: transactionId });
            if (!tx) {
                return res.status(404).json({ msg: "Transaction not found in our system. Please wait 1-2 mins and try again." });
            }
            if (tx.isUsed) {
                return res.status(400).json({ msg: "This Transaction ID has already been used." });
            }
            if (tx.amount < order.amountBDT) {
                return res.status(400).json({ msg: `Insufficient amount. Required: ${order.amountBDT}, Found: ${tx.amount}` });
            }
            
            // Mark TX as used
            tx.isUsed = true;
            tx.usedByUserId = req.user._id.toString();
            await tx.save();
            isVerified = true;
            order.transactionId = transactionId;
        }

        if (isVerified) {
            order.status = 'completed';
            await order.save();

            // Upgrade user
            const user = await User.findById(req.user._id);
            // Add 30 days to current plan expiry or current date if expired
            let currentExpiry = user.planExpiresAt ? new Date(user.planExpiresAt) : new Date();
            if (currentExpiry < new Date()) currentExpiry = new Date(); // If expired, start from today
            
            currentExpiry.setDate(currentExpiry.getDate() + 30); // 30 Days duration
            
            user.planExpiresAt = currentExpiry;
            user.deviceLimit = order.deviceLimit;
            await user.save();

            return res.json({ msg: "Plan successfully upgraded!", newExpiry: user.planExpiresAt, deviceLimit: user.deviceLimit });
        }

    } catch (err) {
        console.error(err.message);
        res.status(500).send('Server Error');
    }
});

// Broadcast Routes
app.get('/api/broadcasts/active', async (req, res) => {
    try {
        const broadcast = await Broadcast.findOne({ isActive: true }).sort({ createdAt: -1 });
        res.json(broadcast);
    } catch (err) {
        console.error(err.message);
        res.status(500).send('Server Error');
    }
});

app.post('/api/broadcasts', [authMiddleware, adminAuthMiddleware], async (req, res) => {
    try {
        const { title, message, imageUrl, link, linkText } = req.body;
        
        // Mark existing broadcasts as inactive
        await Broadcast.updateMany({}, { isActive: false });

        const newBroadcast = new Broadcast({
            title,
            message,
            imageUrl,
            link,
            linkText,
            isActive: true
        });

        const savedBroadcast = await newBroadcast.save();
        res.json(savedBroadcast);
    } catch (err) {
        console.error(err.message);
        res.status(500).send('Server Error');
    }
});

// Login Route (Fix: includes crash fix)
app.post('/api/login', async (req, res) => {
    try {
        // identifier অথবা email দুটোই রিসিভ করার ব্যবস্থা রাখা হলো (সেফটির জন্য)
        const { identifier, email, password, deviceId, deviceName } = req.body; 
        
        const loginId = identifier || email; // যদি identifier না থাকে তবে ইমেইল দেখবে

        if (!loginId) {
            return res.status(400).json({ msg: 'Email/Phone is required' });
        }
        
        let user;
        if (loginId.includes('@')) {
            user = await User.findOne({ email: loginId });
        } else {
            user = await User.findOne({ phone: loginId });
        }

        if (!user) return res.status(400).json({ msg: 'Invalid credentials' });
        
        const isMatch = await bcrypt.compare(password, user.password);
        if (!isMatch) return res.status(400).json({ msg: 'Invalid credentials' });

        if (user.isBlocked) return res.status(403).json({ msg: 'Your account has been blocked' });

        // Device Logic
        if (user.role !== 'admin') {
            // activeSessions অ্যারে না থাকলে তৈরি করে নিই
            if (!user.activeSessions) user.activeSessions = [];
            
            const existingSessionIndex = user.activeSessions.findIndex(s => s.deviceId === deviceId);
            if (existingSessionIndex !== -1) {
                user.activeSessions[existingSessionIndex].deviceName = deviceName || 'Unknown';
                user.activeSessions[existingSessionIndex].loginTime = new Date();
            } else {
                if (!user.deviceLimit) user.deviceLimit = 1; // ডিফল্ট

                if (user.activeSessions.length >= user.deviceLimit) {
                    const sessionDetails = user.activeSessions.map(s => `${s.deviceName}`).join(', ');
                    return res.status(406).json({ 
                        msg: `Login Limit Reached!`,
                        details: `Already active on: ${sessionDetails}. Please logout from other devices.`
                    });
                }
                user.activeSessions.push({
                    deviceId: deviceId || 'unknown_id',
                    deviceName: deviceName || 'Unknown Device',
                    loginTime: new Date()
                });
            }
            await user.save();
        }

        const payload = { user: { id: user.id, role: user.role } };
        jwt.sign(
            payload,
            process.env.JWT_SECRET || 'your_default_secret',
            { expiresIn: '365d' },
            (err, token) => {
                if (err) throw err;
                res.json({ 
                    token, 
                    planExpiresAt: user.planExpiresAt,
                    deviceLimit: user.deviceLimit || 1,
                    activeSessionsCount: user.activeSessions ? user.activeSessions.length : 0,
                    telegramBotToken: user.telegramBotToken,
                    telegramChatId: user.telegramChatId
                });
            }
        );
    } catch (err) {
        console.error(err.message);
        res.status(500).send('Server error');
    }
});

// Register Route
app.post('/api/register', async (req, res) => {
    try {
        const { email, phone, password } = req.body;

        if (!email && !phone) return res.status(400).json({ msg: 'Please provide Email or Phone number' });

        let query = [];
        if (email) query.push({ email });
        if (phone) query.push({ phone });

        let user = await User.findOne({ $or: query });
        if (user) return res.status(400).json({ msg: 'User already exists' });

        const salt = await bcrypt.genSalt(10);
        const hashedPassword = await bcrypt.hash(password, salt);

        user = new User({ 
            email: email || undefined, 
            phone: phone || undefined,
            password: hashedPassword 
        });
        
        await user.save();
        res.status(201).json({ msg: 'User registered with 1 Day Free Trial!' });

    } catch (err) {
        console.error(err.message);
        res.status(500).send('Server error');
    }
});

// Logout
app.post('/api/logout', async (req, res) => {
    try {
        const { email, deviceId } = req.body;
        // ইমেইল না থাকলে শুধু ডিভাইস আইডি দিয়েও ডিলিট করার চেষ্টা করা যায় (অপশনাল)
        if(email) {
            await User.updateOne({ email: email }, { $pull: { activeSessions: { deviceId: deviceId } } });
        }
        res.json({ msg: 'Logged out successfully' });
    } catch (err) {
        res.status(500).send('Server error');
    }
});

// --- ADMIN ROUTES ---
app.get('/api/admin/users', [authMiddleware, adminAuthMiddleware], async (req, res) => {
    try {
        const users = await User.find().select('-password').sort({ createdAt: -1 });
        res.json(users);
    } catch (err) { res.status(500).send('Server error'); }
});

app.post('/api/admin/update-plan/:userId', [authMiddleware, adminAuthMiddleware], async (req, res) => {
    try {
        const { days } = req.body;
        const user = await User.findById(req.params.userId);
        if(!user) return res.status(404).json({msg: 'User not found'});

        let currentExpiry = new Date(user.planExpiresAt || Date.now());
        if (currentExpiry < new Date()) currentExpiry = new Date();

        const newExpiry = new Date(currentExpiry.getTime() + (days * 24 * 60 * 60 * 1000));
        
        user.planExpiresAt = newExpiry;
        await user.save();
        
        res.json({ msg: `Plan extended by ${days} days`, newExpiry });
    } catch (err) { res.status(500).send('Server error'); }
});

app.post('/api/admin/update-limit/:userId', [authMiddleware, adminAuthMiddleware], async (req, res) => {
    try {
        await User.findByIdAndUpdate(req.params.userId, { deviceLimit: req.body.newLimit });
        res.json({ msg: 'Limit updated' });
    } catch (err) { res.status(500).send('Server error'); }
});

app.post('/api/admin/clear-sessions/:userId', [authMiddleware, adminAuthMiddleware], async (req, res) => {
    try {
        await User.findByIdAndUpdate(req.params.userId, { activeSessions: [] });
        res.json({ msg: 'Sessions cleared' });
    } catch (err) { res.status(500).send('Server error'); }
});

app.post('/api/admin/block/:id', [authMiddleware, adminAuthMiddleware], async (req, res) => {
    try {
        await User.findByIdAndUpdate(req.params.id, { isBlocked: true });
        res.json({ msg: 'User blocked' });
    } catch (err) { res.status(500).send('Server error'); }
});

app.post('/api/admin/unblock/:id', [authMiddleware, adminAuthMiddleware], async (req, res) => {
    try {
        await User.findByIdAndUpdate(req.params.id, { isBlocked: false });
        res.json({ msg: 'User unblocked' });
    } catch (err) { res.status(500).send('Server error'); }
});

app.get('/api/admin/user/:userId/messages', [authMiddleware, adminAuthMiddleware], async (req, res) => {
    try {
        const msgs = await Message.find({ userId: req.params.userId, isDeleted: false }).sort({ timestamp: -1 }).limit(50);
        res.json(msgs);
    } catch (err) { res.status(500).send('Server error'); }
});

app.get('/api/admin/user/:userId/call-logs', [authMiddleware, adminAuthMiddleware], async (req, res) => {
    try {
        const logs = await CallLog.find({ userId: req.params.userId }).sort({ date: -1 }).limit(50);
        res.json(logs);
    } catch (err) { res.status(500).send('Server error'); }
});

// --- USER ROUTES ---
app.get('/api/messages', authMiddleware, async (req, res) => {
    try {
        const msgs = await Message.find({ userId: req.user.id, isDeleted: false }).sort({ timestamp: -1 });
        res.json(msgs);
    } catch (err) { res.status(500).send('Server error'); }
});

app.post('/api/messages', authMiddleware, async (req, res) => {
    try {
        const newMsg = new Message({ userId: req.user.id, ...req.body });
        await newMsg.save();
        res.json(newMsg);
    } catch (err) { res.status(500).send('Server error'); }
});

app.post('/api/call-logs/sync', authMiddleware, async (req, res) => {
    try {
        const logs = req.body;
        const userId = req.user.id;
        const ops = logs.map(l => ({
            updateOne: { filter: { userId, date: l.date, number: l.number }, update: { $set: { ...l, userId } }, upsert: true }
        }));
        if(ops.length) await CallLog.bulkWrite(ops);
        res.json({ msg: 'Synced' });
    } catch (err) { res.status(500).send('Server error'); }
});

app.get('/api/call-logs', authMiddleware, async (req, res) => {
    try {
        const logs = await CallLog.find({ userId: req.user.id }).sort({ date: -1 }).limit(100);
        res.json(logs);
    } catch (err) { res.status(500).send('Server error'); }
});

app.post('/api/user/telegram', authMiddleware, async (req, res) => {
    try {
        const { telegramBotToken, telegramChatId } = req.body;
        await User.findByIdAndUpdate(req.user.id, { 
            telegramBotToken: telegramBotToken || "", 
            telegramChatId: telegramChatId || "" 
        });
        res.json({ msg: 'Telegram settings saved' });
    } catch (err) { res.status(500).send('Server error'); }
});

// --- TRASH & DELETE ---
app.post('/api/messages/trash', authMiddleware, async (req, res) => {
    try {
        await Message.updateMany({ _id: { $in: req.body.ids }, userId: req.user.id }, { $set: { isDeleted: true, deletedAt: new Date() } });
        res.json({ msg: 'Trashed' });
    } catch (err) { res.status(500).send('Server error'); }
});

app.post('/api/messages/restore', authMiddleware, async (req, res) => {
    try {
        await Message.updateMany({ _id: { $in: req.body.ids }, userId: req.user.id }, { $set: { isDeleted: false, deletedAt: null } });
        res.json({ msg: 'Restored' });
    } catch (err) { res.status(500).send('Server error'); }
});

app.post('/api/messages/delete', authMiddleware, async (req, res) => {
    try {
        await Message.deleteMany({ _id: { $in: req.body.ids }, userId: req.user.id });
        res.json({ msg: 'Deleted forever' });
    } catch (err) { res.status(500).send('Server error'); }
});

app.get('/api/messages/trash', authMiddleware, async (req, res) => {
    try {
        const msgs = await Message.find({ userId: req.user.id, isDeleted: true }).sort({ deletedAt: -1 });
        res.json(msgs);
    } catch (err) { res.status(500).send('Server error'); }
});

app.post('/api/messages/delete-all', authMiddleware, async (req, res) => {
    try {
        await Message.updateMany({ userId: req.user.id, isDeleted: false }, { $set: { isDeleted: true, deletedAt: new Date() } });
        res.json({ msg: 'All trashed' });
    } catch (err) { res.status(500).send('Server error'); }
});

// --- CRON JOBS ---
const startCronJobs = () => {
    setInterval(async () => {
        try {
            const threeDaysAgo = new Date();
            threeDaysAgo.setDate(threeDaysAgo.getDate() - 3);
            const toArchive = await Message.find({ isDeleted: true, deletedAt: { $lt: threeDaysAgo } });
            if (toArchive.length > 0) {
                const archiveData = toArchive.map(d => ({ ...d.toObject(), originalId: d._id, archivedAt: new Date(), _id: undefined }));
                await ArchivedMessage.insertMany(archiveData);
                await Message.deleteMany({ isDeleted: true, deletedAt: { $lt: threeDaysAgo } });
                console.log(`Auto-Archive: ${archiveData.length} msgs`);
            }
        } catch (e) { console.error("Cron Error", e); }
    }, 86400000);
};

// --- Frontend ---
app.get('/env.js', (req, res) => {
    res.type('application/javascript');
    res.send(`window.env = { API_BASE_URL: "${process.env.PUBLIC_API_URL || ''}" };`);
});
app.get('/admin', (req, res) => {
    res.sendFile(path.join(__dirname, 'public', 'admin.html'));
});

const port = process.env.PORT || 5000;
app.listen(port, () => {
    console.log(`Server running on port ${port}`);
    startCronJobs();
});