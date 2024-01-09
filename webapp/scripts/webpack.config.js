const path = require('path');

module.exports = {
    // The entry point of your application
    entry: './script.js',

    // The output configuration of your bundle
    output: {
        path: path.resolve(__dirname, 'dist'), // Output directory
        filename: 'bundle.js', // Name of the output bundle
    },

    // Configuration for the development server
    devServer: {
        contentBase: '../dist',
    },

    // Module/rules configuration to tell Webpack how to handle different types of modules
    module: {
        rules: [
            {
                test: /\.js$/, // Look for .js files
                exclude: /node_modules/, // Exclude the node_modules directory
                use: {
                    loader: 'babel-loader', // Use babel-loader for these files
                    options: {
                        presets: ['@babel/preset-env'], // Use the env preset
                    },
                },
            },
            {
                test: /\.css$/, // Look for .css files
                use: [
                    'style-loader', // Injects CSS into the DOM via a <style> tag
                    'css-loader', // The css-loader interprets @import and url() like import/require() and will resolve them
                ],
            },
            // Add other rules for other file types as needed
        ],
    },

    // Optional: Configuration for source maps (useful for debugging)
    devtool: 'source-map',
};
