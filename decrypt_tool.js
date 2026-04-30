// decrypt_tool.js
// এই স্ক্রিপ্টটি ব্যবহার করে আপনি আপনার সার্ভারের ডাটাবেজ থেকে এনক্রিপ্ট করা মেসেজ ডিক্রিপ্ট করতে পারবেন।
// রান করার নিয়ম: node decrypt_tool.js

const crypto = require('crypto');

// আপনার লগইন পাসওয়ার্ডটি এখানে দিন
const LOGIN_PASSWORD = "your_login_password_here";

// ডাটাবেজ থেকে পাওয়া এনক্রিপ্ট করা মেসেজ (Base64) এখানে দিন
const ENCRYPTED_MESSAGE = "U2FsdGVkX1..."; 

function decryptMessage(encryptedBase64, password) {
    try {
        const combinedBytes = Buffer.from(encryptedBase64, 'base64');
        const IV_SIZE = 16;
        
        if (combinedBytes.length <= IV_SIZE) return "Invalid data";

        // IV এবং Encrypted Data আলাদা করা
        const ivBytes = combinedBytes.slice(0, IV_SIZE);
        const encryptedBytes = combinedBytes.slice(IV_SIZE);

        // PBKDF2 দিয়ে পাসওয়ার্ড থেকে ৩২ বাইটের শক্তিশালী Key তৈরি করা (১,০০,০০০ ইটারেশন)
        const salt = Buffer.from("ALSMSRECIVE_MILITARY_GRADE_SALT", 'utf8');
        const key = crypto.pbkdf2Sync(password, salt, 100000, 32, 'sha256');

        // AES-256-CBC ডিক্রিপশন
        const decipher = crypto.createDecipheriv('aes-256-cbc', key, ivBytes);
        let decrypted = decipher.update(encryptedBytes);
        decrypted = Buffer.concat([decrypted, decipher.final()]);

        return decrypted.toString('utf8');
    } catch (error) {
        return "Decryption failed: " + error.message;
    }
}

console.log("---- ডিক্রিপশন রেজাল্ট ----");
console.log("আসল মেসেজ:", decryptMessage(ENCRYPTED_MESSAGE, LOGIN_PASSWORD));
console.log("----------------------------");
