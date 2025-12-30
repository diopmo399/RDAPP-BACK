/**
 * Example Application for Cross-Repo E2E Testing
 *
 * Simple Express server for demonstration purposes
 */

const express = require('express');
const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(express.json());

// Health check endpoint
app.get('/api/health', (req, res) => {
  res.status(200).json({
    status: 'healthy',
    timestamp: new Date().toISOString(),
    version: '1.0.0'
  });
});

// Version endpoint
app.get('/api/version', (req, res) => {
  res.status(200).json({
    version: '1.0.0',
    environment: process.env.NODE_ENV || 'development'
  });
});

// Example items endpoint
app.get('/api/items', (req, res) => {
  const items = [
    { id: 1, name: 'Item 1', description: 'First item' },
    { id: 2, name: 'Item 2', description: 'Second item' },
    { id: 3, name: 'Item 3', description: 'Third item' }
  ];

  res.status(200).json(items);
});

// Homepage
app.get('/', (req, res) => {
  res.send(`
    <!DOCTYPE html>
    <html lang="en">
    <head>
      <meta charset="UTF-8">
      <meta name="viewport" content="width=device-width, initial-scale=1.0">
      <title>Example Application</title>
      <style>
        body {
          font-family: Arial, sans-serif;
          max-width: 800px;
          margin: 50px auto;
          padding: 20px;
        }
        h1 { color: #333; }
        .status { color: green; font-weight: bold; }
      </style>
    </head>
    <body>
      <h1>Example Application</h1>
      <p class="status">Status: Running</p>
      <p>This is an example application for cross-repo E2E testing with Cypress.</p>

      <h2>Available Endpoints</h2>
      <ul>
        <li><a href="/api/health">/api/health</a> - Health check</li>
        <li><a href="/api/version">/api/version</a> - Version info</li>
        <li><a href="/api/items">/api/items</a> - Example items</li>
      </ul>
    </body>
    </html>
  `);
});

// 404 handler
app.use((req, res) => {
  res.status(404).json({
    error: 'Not Found',
    path: req.path
  });
});

// Start server
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
  console.log(`Environment: ${process.env.NODE_ENV || 'development'}`);
  console.log(`Health check: http://localhost:${PORT}/api/health`);
});

module.exports = app;
