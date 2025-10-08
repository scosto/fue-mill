import * as vscode from 'vscode';
import * as fs from 'fs';
import * as path from 'path';

export class StatusViewProvider implements vscode.WebviewViewProvider {
    private _view?: vscode.WebviewView;

    constructor(
        private readonly _extensionUri: vscode.Uri,
    ) { }

    public resolveWebviewView(
        webviewView: vscode.WebviewView,
        context: vscode.WebviewViewResolveContext,
        _token: vscode.CancellationToken,
    ) {
        this._view = webviewView;

        webviewView.webview.options = {
            enableScripts: true,
            localResourceRoots: [this._extensionUri]
        };

        webviewView.webview.html = this._getHtmlForWebview(webviewView.webview);

        // Handle messages from the webview
        webviewView.webview.onDidReceiveMessage(data => {
            switch (data.type) {
                case 'radioChanged':
                    this._saveUIConfig({ selectedOption: data.value });
                    break;
                case 'inputChanged':
                    this._saveUIConfig({ textInput: data.value });
                    break;
            }
        });
    }

    public updateValidation(data: any) {
        if (this._view) {
            this._view.webview.postMessage({
                type: 'validationUpdate',
                data: data
            });
        }
    }

    public updateTasty(data: any) {
        if (this._view) {
            this._view.webview.postMessage({
                type: 'tastyUpdate',
                data: data
            });
        }
    }

    private _saveUIConfig(config: any) {
        const workspaceFolder = vscode.workspace.workspaceFolders?.[0];
        if (!workspaceFolder) {
            return;
        }

        const stateDir = path.join(workspaceFolder.uri.fsPath, '.mill-plugin-state');
        const configFile = path.join(stateDir, 'ui-config.json');

        try {
            let currentConfig = {};
            if (fs.existsSync(configFile)) {
                const content = fs.readFileSync(configFile, 'utf8');
                currentConfig = JSON.parse(content);
            }

            const updatedConfig = { ...currentConfig, ...config };
            fs.writeFileSync(configFile, JSON.stringify(updatedConfig, null, 2));
        } catch (error) {
            console.error('Error saving UI config:', error);
        }
    }

    private _getHtmlForWebview(webview: vscode.Webview): string {
        return `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>FUE Mill Plugin Status</title>
    <style>
        html, body {
            height: 100%;
            margin: 0;
            padding: 0;
            overflow: hidden;
        }

        body {
            font-family: var(--vscode-font-family);
            color: var(--vscode-foreground);
            background-color: var(--vscode-sidebar-background);
            display: flex;
            flex-direction: column;
        }

        .logs-container {
            flex: 1;
            display: flex;
            flex-direction: column;
            overflow: hidden;
            min-height: 0;
            position: relative;
        }

        .section {
            margin: 10px;
            border: 1px solid var(--vscode-panel-border);
            border-radius: 4px;
            overflow: hidden;
            display: flex;
            flex-direction: column;
        }

        .logs-container .section {
            min-height: 100px;
        }

        .logs-container .section:first-child {
            height: 50%;
            margin-bottom: 0;
        }

        .logs-container .section:nth-child(3) {
            flex: 1;
            margin-top: 0;
        }

        .splitter {
            height: 6px;
            background-color: var(--vscode-panel-border);
            cursor: ns-resize;
            position: relative;
            z-index: 10;
            margin: 0 10px;
            flex-shrink: 0;
        }

        .splitter:hover {
            background-color: var(--vscode-focusBorder);
        }

        .splitter:active {
            background-color: var(--vscode-focusBorder);
        }

        .config-section {
            flex: 0 0 auto;
        }

        .section-header {
            background-color: var(--vscode-sideBarSectionHeader-background);
            padding: 6px 10px;
            font-weight: bold;
            font-size: 11px;
            text-transform: uppercase;
            border-bottom: 1px solid var(--vscode-panel-border);
            flex-shrink: 0;
        }

        .log-area {
            background-color: var(--vscode-editor-background);
            overflow-y: auto;
            padding: 8px;
            font-family: var(--vscode-editor-font-family);
            font-size: 12px;
            flex: 1;
            min-height: 0;
        }

        .log-line {
            margin: 2px 0;
            white-space: pre-wrap;
            word-break: break-all;
        }

        .log-line.warning {
            color: var(--vscode-editorWarning-foreground);
        }

        .log-line.error {
            color: var(--vscode-editorError-foreground);
        }

        .log-line.success {
            color: var(--vscode-terminal-ansiGreen);
        }

        .controls {
            padding: 10px;
            background-color: var(--vscode-sidebar-background);
        }

        .radio-group {
            margin-bottom: 10px;
        }

        .radio-option {
            margin: 4px 0;
            font-size: 13px;
        }

        .radio-option input[type="radio"] {
            margin-right: 6px;
        }

        .text-input {
            width: 100%;
            padding: 4px 6px;
            background-color: var(--vscode-input-background);
            color: var(--vscode-input-foreground);
            border: 1px solid var(--vscode-input-border);
            border-radius: 2px;
            font-family: var(--vscode-font-family);
            font-size: 13px;
            box-sizing: border-box;
        }

        .text-input:focus {
            outline: 1px solid var(--vscode-focusBorder);
        }
    </style>
</head>
<body>
    <div class="logs-container">
        <div class="section" id="validation-section">
            <div class="section-header">Regel-Validierung</div>
            <div class="log-area" id="validation-log">
                <div class="log-line">Waiting for validation data...</div>
            </div>
        </div>

        <div class="splitter" id="splitter"></div>

        <div class="section" id="tasty-section">
            <div class="section-header">TASTy-Transformation</div>
            <div class="log-area" id="tasty-log">
                <div class="log-line">Waiting for transformation data...</div>
            </div>
        </div>
    </div>

    <div class="section config-section">
        <div class="section-header">Configuration</div>
        <div class="controls">
            <div class="radio-group">
                <div class="radio-option">
                    <input type="radio" id="optionA" name="option" value="a" checked>
                    <label for="optionA">Option Alfons Steinhoff</label>
                </div>
                <div class="radio-option">
                    <input type="radio" id="optionB" name="option" value="b">
                    <label for="optionB">Option B</label>
                </div>
                <div class="radio-option">
                    <input type="radio" id="optionC" name="option" value="c">
                    <label for="optionC">Option C</label>
                </div>
            </div>
            <input type="text" class="text-input" id="textInput" placeholder="Enter configuration value...">
        </div>
    </div>

    <script>
        const vscode = acquireVsCodeApi();

        // Resizable splitter functionality
        (function() {
            const splitter = document.getElementById('splitter');
            const validationSection = document.getElementById('validation-section');
            const tastySection = document.getElementById('tasty-section');
            const logsContainer = document.querySelector('.logs-container');

            let isResizing = false;
            let startY = 0;
            let startHeight = 0;

            splitter.addEventListener('mousedown', (e) => {
                isResizing = true;
                startY = e.clientY;
                startHeight = validationSection.offsetHeight;
                document.body.style.cursor = 'ns-resize';
                e.preventDefault();
            });

            document.addEventListener('mousemove', (e) => {
                if (!isResizing) return;

                const containerHeight = logsContainer.offsetHeight;
                const delta = e.clientY - startY;
                const newHeight = startHeight + delta;
                const minHeight = 100;
                const maxHeight = containerHeight - minHeight - splitter.offsetHeight - 20; // 20px for margins

                if (newHeight >= minHeight && newHeight <= maxHeight) {
                    const percentage = (newHeight / containerHeight) * 100;
                    validationSection.style.height = percentage + '%';
                }
            });

            document.addEventListener('mouseup', () => {
                if (isResizing) {
                    isResizing = false;
                    document.body.style.cursor = '';
                }
            });
        })();

        // Handle messages from extension
        window.addEventListener('message', event => {
            const message = event.data;

            switch (message.type) {
                case 'validationUpdate':
                    updateValidationLog(message.data);
                    break;
                case 'tastyUpdate':
                    updateTastyLog(message.data);
                    break;
            }
        });

        // Send radio button changes
        document.querySelectorAll('input[name="option"]').forEach(radio => {
            radio.addEventListener('change', (e) => {
                vscode.postMessage({
                    type: 'radioChanged',
                    value: e.target.value
                });
            });
        });

        // Send text input changes (debounced)
        let inputTimeout;
        document.getElementById('textInput').addEventListener('input', (e) => {
            clearTimeout(inputTimeout);
            inputTimeout = setTimeout(() => {
                vscode.postMessage({
                    type: 'inputChanged',
                    value: e.target.value
                });
            }, 500);
        });

        function updateValidationLog(data) {
            const logArea = document.getElementById('validation-log');
            logArea.innerHTML = '';

            if (data.logs && data.logs.length > 0) {
                data.logs.forEach(log => {
                    const line = document.createElement('div');
                    line.className = 'log-line ' + (log.level || '');
                    line.textContent = log.message;
                    logArea.appendChild(line);
                });
            } else {
                const line = document.createElement('div');
                line.className = 'log-line';
                line.textContent = 'No validation messages';
                logArea.appendChild(line);
            }

            // Auto-scroll to bottom
            logArea.scrollTop = logArea.scrollHeight;
        }

        function updateTastyLog(data) {
            const logArea = document.getElementById('tasty-log');
            logArea.innerHTML = '';

            if (data.logs && data.logs.length > 0) {
                data.logs.forEach(log => {
                    const line = document.createElement('div');
                    line.className = 'log-line ' + (log.level || '');
                    line.textContent = log.message;
                    logArea.appendChild(line);
                });
            } else {
                const line = document.createElement('div');
                line.className = 'log-line';
                line.textContent = 'No transformation messages';
                logArea.appendChild(line);
            }

            // Auto-scroll to bottom
            logArea.scrollTop = logArea.scrollHeight;
        }
    </script>
</body>
</html>`;
    }
}
