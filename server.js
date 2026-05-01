const express = require('express');
const mongoose = require('mongoose');
const path = require('path');
const bodyParser = require('body-parser');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const cors = require('cors');
require('dotenv').config();

const app = express();
app.set('trust proxy', 1); // Fix: Prevent rate limiter from blocking everyone if behind Nginx/Cloudflare
app.use(bodyParser.json({ limit: '50mb' }));
app.use(bodyParser.urlencoded({ limit: '50mb', extended: true }));
app.use(express.static(path.join(__dirname, 'public')));

// CORS — only allow specific origins
const allowedOrigins = [
    'http://localhost:3000',
    process.env.ADMIN_ORIGIN || 'http://localhost:5000'
];
app.use(cors({
    origin: function(origin, callback) {
        // Allow mobile apps (no origin) and allowed origins
        if (!origin || allowedOrigins.includes(origin)) {
            callback(null, true);
        } else {
            callback(null, true); // Still allow for now — tighten after HTTPS
        }
    },
    methods: ['GET', 'POST', 'PUT', 'DELETE'],
    allowedHeaders: ['Content-Type', 'x-auth-token', 'x-device-id', 'x-webhook-secret']
}));

// Security headers
app.use((req, res, next) => {
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('X-XSS-Protection', '1; mode=block');
    next();
});

// Rate limiter for login (brute force protection)
const loginAttempts = new Map();
const rateLimitLogin = (req, res, next) => {
    const ip = req.ip || req.connection.remoteAddress;
    const now = Date.now();
    const windowMs = 15 * 60 * 1000; // 15 minutes
    const maxAttempts = 10;

    if (!loginAttempts.has(ip)) {
        loginAttempts.set(ip, { count: 1, firstAttempt: now });
        return next();
    }
    const record = loginAttempts.get(ip);
    if (now - record.firstAttempt > windowMs) {
        loginAttempts.set(ip, { count: 1, firstAttempt: now });
        return next();
    }
    if (record.count >= maxAttempts) {
        const waitMins = Math.ceil((windowMs - (now - record.firstAttempt)) / 60000);
        return res.status(429).json({ msg: `Too many attempts. Try again in ${waitMins} minutes.` });
    }
    record.count++;
    next();
};

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
    transactionId: { type: String, default: "" },
    referenceId: { type: String, default: "" },
    createdAt: { type: Date, default: Date.now },
    expiresAt: { type: Date, default: () => Date.now() + 60*60*1000 }
});
const Order = mongoose.model('Order', OrderSchema);

const TransactionSchema = new mongoose.Schema({
    transactionId: { type: String, required: true, unique: true },
    amount: { type: Number, required: true },
    sender: { type: String },
    method: { type: String, enum: ['bkash', 'nagad', 'binance'], required: true },
    reference: { type: String, default: "" },
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
        const jwtSecret = process.env.JWT_SECRET;
        if (!jwtSecret) {
            console.error('[CRITICAL] JWT_SECRET not set in .env!');
            return res.status(500).json({ msg: 'Server misconfiguration: JWT_SECRET missing' });
        }
        const decoded = jwt.verify(token, jwtSecret);
        const user = await User.findById(decoded.user.id);
        
        if (!user) return res.status(401).json({ msg: 'User not found' });
        if (user.isBlocked) return res.status(403).json({ msg: 'Your account has been blocked.' });

        // Plan check and Device Session validation (skip for admin)
        if (user.role !== 'admin') {
            // 1. Validate if device session is still active
            const reqDeviceId = req.header('x-device-id');
            // We enforce session validation if x-device-id is present. 
            // (Mobile app always sends this. Skipping check if absent ensures web frontend isn't locked out immediately if it doesn't send it, but we still protect mobile sessions)
            if (reqDeviceId) {
                const sessionExists = user.activeSessions && user.activeSessions.some(s => s.deviceId === reqDeviceId);
                if (!sessionExists) {
                    return res.status(401).json({ msg: 'Session expired or revoked. Please login again.' });
                }
            }

            // 2. Plan check (bypass for specific routes needed for renewal)
            if (user.planExpiresAt && new Date() > new Date(user.planExpiresAt)) {
                const allowedPaths = [
                    '/api/plans/create-order',
                    '/api/plans/verify-order',
                    '/api/payment-info',
                    '/api/user/profile',
                    '/api/logout',
                    '/api/user/telegram'
                ];
                
                // Allow if req.path exactly matches or starts with an allowed path
                const isAllowed = allowedPaths.some(p => req.path === p || req.path.startsWith(p));
                if (!isAllowed) {
                    return res.status(402).json({ msg: 'Plan Expired! Please renew subscription.' });
                }
            }
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
    // NOTE: Do NOT log full headers/body — secret_key would be exposed in logs
    
    try {
        // The external app sends structured JSON data directly
        const { secret_key, amount, trx_id, provider, sender, reference, ref_code, full_sms } = req.body;

        // Cast to string to prevent NoSQL injection (e.g. { "$ne": null })
        const safeTrxId = trx_id ? String(trx_id) : null;
        const safeFullSms = full_sms ? String(full_sms) : "";
        const safeReference = reference ? String(reference) : "";
        const safeRefCode = ref_code ? String(ref_code) : "";

        // Extract reference ID from explicit field or parse from full SMS text
        const refMatch = safeFullSms ? safeFullSms.match(/AL-PAY-[A-Z0-9]+/) : null;
        const parsedRef = safeReference || safeRefCode || (refMatch ? refMatch[0] : "");

        // Verify Secret Key — only accept from .env
        const expectedKey = process.env.WEBHOOK_SECRET;
        if (!expectedKey || secret_key !== expectedKey) {
            return res.status(401).json({ msg: 'Invalid or unconfigured secret key' });
        }

        if (!safeTrxId || !amount) {
            return res.status(400).json({ msg: "trx_id and amount are required" });
        }

        const parsedAmount = parseFloat(amount);
        if (isNaN(parsedAmount) || parsedAmount <= 0) {
            return res.status(400).json({ msg: "Invalid amount" });
        }

        // Detect Method from provider
        let method = 'bkash';
        const lowerProvider = (provider ? String(provider) : "").toLowerCase();
        const lowerSender = (sender ? String(sender) : "").toLowerCase();
        
        if (lowerProvider.includes('nagad') || lowerSender.includes('nagad')) {
            method = 'nagad';
        } else if (lowerProvider.includes('binance') || lowerSender.includes('binance')) {
            method = 'binance';
        }

        // Check if transaction already exists
        const existingTx = await Transaction.findOne({ transactionId: safeTrxId });
        if (existingTx) {
            return res.status(200).json({ msg: "Transaction already exists" });
        }

        // Save new transaction
        const newTx = new Transaction({
            transactionId: safeTrxId,
            amount: parsedAmount,
            sender: lowerSender || "Unknown",
            method: method,
            reference: parsedRef
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
        
        if (req.user.role !== 'admin' && req.user.planExpiresAt && new Date() < new Date(req.user.planExpiresAt)) {
            if (deviceLimit < req.user.deviceLimit) {
                return res.status(400).json({ msg: "Cannot downgrade plan while current plan is active." });
            }
        }

        const cryptoObj = require('crypto');
        const referenceId = 'AL-PAY-' + cryptoObj.randomBytes(4).toString('hex').toUpperCase();

        const newOrder = new Order({
            userId: req.user._id,
            planType,
            amountBDT,
            amountUSDT,
            deviceLimit,
            paymentMethod: paymentMethod || 'none',
            referenceId
        });

        const savedOrder = await newOrder.save();
        res.json({ msg: "Order created", orderId: savedOrder._id, referenceId, amountBDT, amountUSDT });
    } catch (err) {
        console.error(err.message);
        res.status(500).send('Server Error');
    }
});

// For Binance Verification, we import needed modules
const crypto = require('crypto');
const axios = require('axios');

async function verifyBinanceTransaction(orderIdToMatch, expectedAmountUSDT) {
    // IMPORTANT: Binance keys must be in .env file — never hardcode
    const BINANCE_API_KEY = process.env.BINANCE_API_KEY;
    const BINANCE_SECRET_KEY = process.env.BINANCE_SECRET_KEY;
    if (!BINANCE_API_KEY || !BINANCE_SECRET_KEY) {
        console.error('[Binance] API keys not set in .env');
        return false;
    }
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
                if (amountFloat >= (expectedAmountUSDT - 0.05)) {
                    // Check if the reference ID is somewhere in the Binance transaction data (e.g., Note/Remark)
                    if (JSON.stringify(tx).includes(orderIdToMatch)) {
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
        const { orderId } = req.body; // transactionId is no longer needed from user

        const order = await Order.findById(orderId);
        if (!order) return res.status(404).json({ msg: "Order not found" });
        if (order.userId.toString() !== req.user._id.toString()) return res.status(403).json({ msg: "Unauthorized order access" });
        if (order.status !== 'pending') return res.status(400).json({ msg: "Order is already " + order.status });
        if (new Date() > new Date(order.expiresAt)) {
            order.status = 'expired';
            await order.save();
            return res.status(400).json({ msg: "Order has expired. Please create a new one." });
        }

        let isVerified = false;

        if (order.paymentMethod === 'binance') {
            isVerified = await verifyBinanceTransaction(order.referenceId, order.amountUSDT);
            if (!isVerified) {
                return res.status(400).json({ msg: "Binance payment not found. Make sure you included the Reference ID in the Note/Remark." });
            }
            order.transactionId = order.referenceId;
        } else {
            // Local Webhook DB check (Bkash/Nagad) matching reference
            const tx = await Transaction.findOne({ reference: order.referenceId, method: order.paymentMethod });
            if (!tx) {
                return res.status(404).json({ msg: "Payment not found. Please ensure you used the exact Reference ID and wait 1-2 mins." });
            }
            if (tx.isUsed) {
                return res.status(400).json({ msg: "This payment has already been used." });
            }
            if (tx.amount < order.amountBDT) {
                return res.status(400).json({ msg: `Insufficient amount. Required: ${order.amountBDT}, Found: ${tx.amount}` });
            }
            
            // Mark TX as used
            tx.isUsed = true;
            tx.usedByUserId = req.user._id.toString();
            await tx.save();
            isVerified = true;
            order.transactionId = tx.transactionId; // Save the actual provider TrxID
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

            // Handle active sessions overflow on downgrade
            if (user.activeSessions && user.activeSessions.length > order.deviceLimit) {
                user.activeSessions = user.activeSessions.slice(-order.deviceLimit);
            }

            await user.save();

            const activeCount = user.activeSessions ? user.activeSessions.length : 0;
            return res.json({ msg: "Plan successfully upgraded!", newExpiry: user.planExpiresAt, deviceLimit: user.deviceLimit, activeSessionsCount: activeCount });
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
app.post('/api/login', rateLimitLogin, async (req, res) => {
    try {
        // identifier অথবা email দুটোই রিসিভ করার ব্যবস্থা রাখা হলো (সেফটির জন্য)
        const { identifier, email, password, deviceId, deviceName } = req.body; 
        
        // NoSQL Injection Prevention: Cast to String
        const loginIdRaw = identifier || email; // যদি identifier না থাকে তবে ইমেইল দেখবে
        const loginId = loginIdRaw ? String(loginIdRaw) : null;
        const safePassword = password ? String(password) : "";
        const safeDeviceId = deviceId ? String(deviceId) : 'unknown_id';
        const safeDeviceName = deviceName ? String(deviceName) : 'Unknown Device';

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
        
        const isMatch = await bcrypt.compare(safePassword, user.password);
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
                    deviceId: safeDeviceId,
                    deviceName: safeDeviceName,
                    loginTime: new Date()
                });
            }
            await user.save();
        }

        const jwtSecret = process.env.JWT_SECRET;
        if (!jwtSecret) return res.status(500).json({ msg: 'Server misconfiguration: JWT_SECRET missing' });

        const payload = { user: { id: user.id, role: user.role } };
        jwt.sign(
            payload,
            jwtSecret,
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
app.post('/api/register', rateLimitLogin, async (req, res) => {
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
        const { email, phone, deviceId } = req.body;
        
        // NoSQL Injection Prevention: Cast to String
        const safeEmail = email ? String(email) : undefined;
        const safePhone = phone ? String(phone) : undefined;
        const safeDeviceId = deviceId ? String(deviceId) : undefined;
        
        // Support both email and phone users
        if (safeEmail) {
            await User.updateOne({ email: safeEmail }, { $pull: { activeSessions: { deviceId: safeDeviceId } } });
        } else if (safePhone) {
            await User.updateOne({ phone: safePhone }, { $pull: { activeSessions: { deviceId: safeDeviceId } } });
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
app.get('/api/user/profile', authMiddleware, async (req, res) => {
    try {
        const user = await User.findById(req.user.id).select('-password');
        const messageCount = await Message.countDocuments({ userId: req.user.id, isDeleted: false });
        const callLogCount = await CallLog.countDocuments({ userId: req.user.id });
        const trashedCount = await Message.countDocuments({ userId: req.user.id, isDeleted: true });

        res.json({
            email: user.email || null,
            phone: user.phone || null,
            role: user.role,
            createdAt: user.createdAt,
            planExpiresAt: user.planExpiresAt,
            deviceLimit: user.deviceLimit,
            activeSessions: user.activeSessions ? user.activeSessions.length : 0,
            activeDevices: user.activeSessions ? user.activeSessions.map(s => ({ deviceName: s.deviceName, loginTime: s.loginTime })) : [],
            hasTelegram: !!(user.telegramBotToken && user.telegramChatId),
            stats: {
                totalMessages: messageCount,
                totalCallLogs: callLogCount,
                trashedMessages: trashedCount
            }
        });
    } catch (err) {
        res.status(500).send('Server error');
    }
});

app.get('/api/messages', authMiddleware, async (req, res) => {
    try {
        const msgs = await Message.find({ userId: req.user.id, isDeleted: false }).sort({ timestamp: -1 });
        res.json(msgs);
    } catch (err) { res.status(500).send('Server error'); }
});

app.post('/api/messages', authMiddleware, async (req, res) => {
    try {
        // SECURITY: Explicitly extract known fields — prevents userId override attack
        // App sends: type, sender, message, deviceId, deviceName, timestamp
        const { type, sender, message, deviceId, deviceName, timestamp, simSlot, packageName } = req.body;
        const newMsg = new Message({
            userId: req.user.id,        // Always from auth token — never from body
            type: type || 'sms',
            sender: sender || 'Unknown',
            message: message || '',
            deviceId: deviceId || 'unknown',
            deviceName: deviceName || 'Unknown Device',
            timestamp: timestamp || new Date().toISOString(),
            simSlot: simSlot,
            packageName: packageName,
            isDeleted: false            // Always false on create
        });
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
// --- Payment Info Endpoint ---
app.get('/api/payment-info', authMiddleware, (req, res) => {
    res.json({
        bkash: "01981475404",
        nagad: "01981475404",
        binance: "852778644"
    });
});

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
