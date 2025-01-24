import {getBlocker} from "../globals/elements";
import {Modal} from "./modal";

let shortcutModal: Modal | null = null;

function getShortcutModal(): Modal {
    if (!shortcutModal) {
        const shortcutsDialog = document.getElementById('shortcuts-dialog');
        if (!(shortcutsDialog instanceof HTMLDivElement)) {
            throw new Error('Shortcuts dialog not found');
        }
        shortcutModal = new Modal(shortcutsDialog, getBlocker());
    }
    return shortcutModal;
}

function getShortcutMap(): Map<string, string> {
    return new Map([
        ["Right Click", "Context Menu"],
        ["Tab", "Next Input"],
        ["Shift + Tab", "Previous Input"],
        ["Ctrl + Z", "Undo"],
        ["Ctrl + Shift + Z", "Redo"],
        ["Ctrl + C", "Copy"],
        ["Ctrl + X", "Cut"],
        ["Ctrl + V", "Paste"],
    ]);
}

export function showShortcutsDialog(): void {
    const shortcutsDialogContents = document.getElementById('shortcuts-dialog-contents');
    if (!shortcutsDialogContents) {
        throw new Error('Shortcuts dialog contents not found');
    }

    shortcutsDialogContents.replaceChildren(generateShortcutsTable());
    getShortcutModal().show();
}

function generateShortcutsTable(): HTMLTableElement {
    const shortcutsTable = document.createElement('table');
    const shortcuts = getShortcutMap();
    for (const [shortcut, action] of shortcuts) {
        const shortcutRow = document.createElement('tr');
        const shortcutCell = document.createElement('td');
        const actionCell = document.createElement('td');
        shortcutCell.textContent = shortcut;
        actionCell.textContent = action;
        shortcutRow.appendChild(shortcutCell);
        shortcutRow.appendChild(actionCell);
        shortcutsTable.appendChild(shortcutRow);
    }
    return shortcutsTable;
}
