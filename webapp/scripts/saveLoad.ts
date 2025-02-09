import {lastNodeString, loadTreeFromString, setSelectedMode} from "./treeManipulation";
import {getSelectedMode} from "./utils";
import {displayError} from "./components/displayError";
import {markHasUsedLangSelector} from "./attention";
import {getCurrentLanguage, setCurrentLanguage} from "./langSelector";

const fileInput: HTMLInputElement = document.createElement('input');

export function saveTree(): void {
    const contents = JSON.stringify({
        nodeString: lastNodeString,
        lang: getCurrentLanguage(),
        mode: getSelectedMode(),
    })
    const blob = new Blob([contents], {type: 'text/plain'});
    const url = window.URL.createObjectURL(blob);

    const a = document.createElement('a');
    a.href = url;
    a.download = 'tree.cdtree';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);

    window.URL.revokeObjectURL(url);
}

export function setupFileInput(): void {
    fileInput.type = 'file';
    fileInput.accept = '.cdtree';
    fileInput.onchange = event => {
        const files = (event.target as HTMLInputElement).files;
        if (!files || files.length === 0) throw new Error('No file selected');
        const file = files[0];
        const reader = new FileReader();
        reader.onload = () => loadFromFile(reader);
        reader.readAsText(file);
    };
}

export function setupFileDragAndDrop(): void {
    const treeContainer = document.getElementById('tree-container');
    if (!treeContainer) {
        console.error('Tree container not found');
        return;
    }

    const highlightClass: string = 'file-drag-highlight';
    const addHighlight = () => treeContainer.classList.add(highlightClass);
    const removeHighlight = () => treeContainer.classList.remove(highlightClass);

    treeContainer.addEventListener('dragover', (event) => {
        if (fileDragAndDropPaused) return;

        event.preventDefault();
        addHighlight();
    });

    treeContainer.addEventListener('dragleave', removeHighlight);

    treeContainer.addEventListener('drop', (event) => {
        if (fileDragAndDropPaused) return;

        event.preventDefault();
        removeHighlight();

        const file: File | undefined = event.dataTransfer?.files[0];
        if (!file) {
            displayError('No file dropped');
            return;
        }

        const reader = new FileReader();
        reader.onload = () => loadFromFile(reader);
        reader.readAsText(file);
    });
}

function loadFromFile(reader: FileReader): void {
    reader.onerror = () => displayError(new Error('Error occurred while attempting to read file'))
    try {
        const contents: string = reader.result as string;
        const json = JSON.parse(contents);
        if (!json.nodeString || !json.lang || !json.mode) {
            throw new Error('Provided file did not contain required tree data');
        }
        if (json.lang !== getCurrentLanguage()) {
            markHasUsedLangSelector();
        }
        setCurrentLanguage(json.lang);
        setSelectedMode(json.mode);
        loadTreeFromString(json.nodeString);
    } catch (e) {
        if (e instanceof SyntaxError) {
            e = new SyntaxError('Provided file was not valid JSON');
        }
        displayError(e);
    }
}

export function loadTree(): void {
    fileInput.click();
}

let fileDragAndDropPaused: boolean = false;

export function pauseFileDragAndDrop(): void {
    fileDragAndDropPaused = true;
}

export function resumeFileDragAndDrop(): void {
    fileDragAndDropPaused = false;
}
