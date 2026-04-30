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