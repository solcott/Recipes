;(function(config) {
  // CORS headers required for SharedArrayBuffer (SQLite WASM worker)
  config.devServer.headers = [
      { key: 'Cross-Origin-Opener-Policy', value: 'same-origin' },
      { key: 'Cross-Origin-Embedder-Policy', value: 'require-corp' }
  ]

  // Ensure dynamically loaded chunks resolve from the root regardless of URL depth
  config.output = config.output || {}
  config.output.publicPath = "/"

  // Serve a modified index.html that injects <base href="/"> for every HTML request.
  //
  // Problem: the Kotlin build generates index.html with relative asset paths
  // (e.g. <script src="webApp.js">). When the dev server serves that HTML at
  // /recipes/category/Chicken, the browser resolves the script as
  // /recipes/category/webApp.js → 404.
  //
  // Solution: intercept all text/html requests before webpack's own middlewares,
  // read the real index.html, inject <base href="/"> into <head>, and serve it.
  // The base tag makes every relative path resolve from the root regardless of
  // the current URL depth.
  config.devServer.setupMiddlewares = function(middlewares, devServer) {
    const path = require('path')
    const fs   = require('fs')
    const htmlFile = path.resolve(__dirname, 'kotlin/index.html')

    middlewares.unshift({
      name: 'spa-base-href',
      middleware: function(req, res, next) {
        const accept = req.headers.accept || ''
        const urlPath = req.url.split('?')[0]
        const lastSegment = urlPath.split('/').pop()
        const looksLikeFile = lastSegment.includes('.')

        if (accept.includes('text/html') && !looksLikeFile) {
          try {
            let html = fs.readFileSync(htmlFile, 'utf-8')
            html = html.replace('<head>', '<head>\n    <base href="/">')
            res.writeHead(200, {
              'Content-Type': 'text/html; charset=utf-8',
              'Cross-Origin-Opener-Policy': 'same-origin',
              'Cross-Origin-Embedder-Policy': 'require-corp',
            })
            res.end(html)
          } catch (e) {
            next()
          }
        } else {
          next()
        }
      }
    })
    return middlewares
  }
})(config);
