import express from 'express';
import path from 'path';
import { fileURLToPath } from 'url';
import { createServer as createViteServer } from 'vite';
import archiver from 'archiver';
import fs from 'fs';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

async function startServer() {
  const app = express();
  const PORT = 3000;

  // API Route for exporting the project as a ZIP
  app.get('/api/export', (req, res) => {
    const archive = archiver('zip', {
      zlib: { level: 9 } // Sets the compression level.
    });

    res.attachment('project-export.zip');

    archive.on('error', (err) => {
      res.status(500).send({ error: err.message });
    });

    archive.pipe(res);

    // Add files to the archive
    const rootDir = process.cwd();
    
    // List of files/directories to include
    const includes = [
      'src',
      'public',
      'package.json',
      'tsconfig.json',
      'vite.config.ts',
      'index.html',
      '.env.example',
      '.gitignore',
      'metadata.json'
    ];

    includes.forEach(item => {
      const fullPath = path.join(rootDir, item);
      if (fs.existsSync(fullPath)) {
        const stats = fs.statSync(fullPath);
        if (stats.isDirectory()) {
          archive.directory(fullPath, item);
        } else {
          archive.file(fullPath, { name: item });
        }
      }
    });

    archive.finalize();
  });

  // Vite middleware for development
  if (process.env.NODE_ENV !== 'production') {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: 'spa',
    });
    app.use(vite.middlewares);
  } else {
    const distPath = path.join(process.cwd(), 'dist');
    app.use(express.static(distPath));
    app.get('*', (req, res) => {
      res.sendFile(path.join(distPath, 'index.html'));
    });
  }

  app.listen(PORT, '0.0.0.0', () => {
    console.log(`Server running on http://localhost:${PORT}`);
  });
}

startServer();
