const mongoose = require('mongoose');
require('dotenv').config();

const dbURI = process.env.MONGODB_URI || "mongodb://localhost:27017/alsmsrecive";

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

async function addDemoBroadcast() {
    try {
        await mongoose.connect(dbURI);
        console.log('MongoDB connected...');

        // আগের সব ব্রডকাস্ট অফ করে দেওয়া হচ্ছে
        await Broadcast.updateMany({}, { isActive: false });

        const newBroadcast = new Broadcast({
            title: "🚀 App Update Available (v2.0)",
            message: "We have launched a brand new feature for multi-device sync. Please update your app from our official website to continue enjoying the services without interruption.",
            imageUrl: "https://tits-sb.com/uploads/demo_banner.png", // Demo image
            link: "https://t.me/your_channel", // এখানে আপনার লিংক দিবেন
            linkText: "Download Update",
            isActive: true
        });

        await newBroadcast.save();
        console.log('✅ Demo Broadcast Successfully Added!');
        
        mongoose.connection.close();
    } catch (err) {
        console.error('❌ Error adding broadcast:', err);
        mongoose.connection.close();
    }
}

addDemoBroadcast();
