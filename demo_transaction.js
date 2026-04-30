const mongoose = require('mongoose');

const dbURI = "mongodb://localhost:27017/alsmsrecive"; // Change if different on VPS

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

async function addDemoTx() {
    await mongoose.connect(dbURI);
    
    try {
        await new Transaction({
            transactionId: "ALJRVSRD456TSGS",
            amount: 49,
            sender: "bKash",
            method: "bkash",
            isUsed: false
        }).save();
        console.log("✅ Demo Transaction Added! Now try verifying with ALJRVSRD456TSGS in the app.");
    } catch(e) {
        if(e.code === 11000) console.log("Transaction already exists!");
        else console.log(e);
    }
    process.exit();
}
addDemoTx();
