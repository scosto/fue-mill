import * as vscode from 'vscode';
import * as fs from 'fs';
import * as path from 'path';
import { StatusViewProvider } from './statusView';

let statusViewProvider: StatusViewProvider | undefined;

export function activate(context: vscode.ExtensionContext) {
    console.log('FUE Mill Status extension activated');

    // Register the webview view provider
    statusViewProvider = new StatusViewProvider(context.extensionUri);

    context.subscriptions.push(
        vscode.window.registerWebviewViewProvider(
            'fue-mill.statusView',
            statusViewProvider
        )
    );

    // Setup file watchers
    setupFileWatchers(context);
}

function setupFileWatchers(context: vscode.ExtensionContext) {
    const workspaceFolder = vscode.workspace.workspaceFolders?.[0];
    if (!workspaceFolder) {
        return;
    }

    const stateDir = path.join(workspaceFolder.uri.fsPath, '.mill-plugin-state');

    // Ensure state directory exists
    if (!fs.existsSync(stateDir)) {
        fs.mkdirSync(stateDir, { recursive: true });
    }

    // Watch validation status file
    const validationFile = path.join(stateDir, 'validation.json');
    const validationWatcher = fs.watch(validationFile, (eventType) => {
        if (eventType === 'change') {
            updateValidationStatus(validationFile);
        }
    });

    // Watch tasty transform status file
    const tastyFile = path.join(stateDir, 'tasty.json');
    const tastyWatcher = fs.watch(tastyFile, (eventType) => {
        if (eventType === 'change') {
            updateTastyStatus(tastyFile);
        }
    });

    context.subscriptions.push({
        dispose: () => {
            validationWatcher.close();
            tastyWatcher.close();
        }
    });

    // Initial load
    if (fs.existsSync(validationFile)) {
        updateValidationStatus(validationFile);
    }
    if (fs.existsSync(tastyFile)) {
        updateTastyStatus(tastyFile);
    }
}

function updateValidationStatus(filePath: string) {
    try {
        const content = fs.readFileSync(filePath, 'utf8');
        const data = JSON.parse(content);
        statusViewProvider?.updateValidation(data);
    } catch (error) {
        console.error('Error reading validation status:', error);
    }
}

function updateTastyStatus(filePath: string) {
    try {
        const content = fs.readFileSync(filePath, 'utf8');
        const data = JSON.parse(content);
        statusViewProvider?.updateTasty(data);
    } catch (error) {
        console.error('Error reading tasty status:', error);
    }
}

export function deactivate() {}
