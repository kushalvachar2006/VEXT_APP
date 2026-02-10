const express = require("express");
const multer = require("multer");
const cors = require("cors");
const fs = require("fs");
const path = require("path");

const app = express();
app.use(cors());

const PORT = process.env.PORT || 3000;
const UPLOAD_DIR = path.join(__dirname, "uploads");

// Create uploads folder if not exists
if (!fs.existsSync(UPLOAD_DIR)) {
    fs.mkdirSync(UPLOAD_DIR);
}

// Configure storage
const storage = multer.diskStorage({
    destination: function (req, file, cb) {
        cb(null, UPLOAD_DIR);
    },
    filename: function (req, file, cb) {
        const uniqueName = Date.now() + "-" + file.originalname;
        cb(null, uniqueName);
    }
});

const upload = multer({
    storage,
    limits: { fileSize: 50 * 1024 * 1024 } // 50MB limit
});

// 🔹 Upload endpoint
app.post("/upload", upload.single("file"), (req, res) => {
    if (!req.file) {
        return res.status(400).json({ error: "No file uploaded" });
    }

    const baseUrl = `${req.protocol}://${req.get("host")}`;
    const fileUrl = `${baseUrl}/download/${req.file.filename}`;

    res.json({
        fileName: req.file.filename,
        fileUrl: fileUrl
    });
});

// 🔹 Download + Delete
app.get("/download/:filename", (req, res) => {
    const filePath = path.join(UPLOAD_DIR, req.params.filename);

    if (!fs.existsSync(filePath)) {
        return res.status(404).json({ error: "File not found" });
    }

    res.download(filePath, (err) => {
        if (!err) {
            fs.unlink(filePath, () => {
                console.log("Deleted after download:", req.params.filename);
            });
        }
    });
});

// Health check
app.get("/", (req, res) => {
    res.send("Storage server running 🚀");
});

app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
