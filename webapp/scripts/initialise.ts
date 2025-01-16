import '../styles/main.sass';
import {
    clearTreeNode,
    contextMenuCopy,
    contextMenuPaste,
    doStartNodeBlank,
    exampleLiteralChanged,
    handleLiteralChanged,
    resetCopyCache,
    startNodeBlank
} from "./actions";
import {getTree, redo, resetTreeManipulation, undo, updateTextInputWidth} from "./treeManipulation";
import {handleTabPressed, resetInterfaceGlobals, toggleControls} from "./interface";
import {loadImages} from "./imageLoading";
import {setupExampleSelector} from "./components/customExprSelector";
import {loadTree, saveTree} from "./saveLoad";
import {
    getContextMenuZoomToFitButton,
    getCopyButton,
    getDeleteButton,
    getExportCloseButton,
    getExportCopyButton,
    getExportLatexButton,
    getLoadButton,
    getPasteButton,
    getRedoButton,
    getSaveButton,
    getToggleControlsButton,
    getUndoButton,
    getZoomToFitButton
} from "./globals/elements";
import {setUpPanZoom, zoomToFit} from "./components/panzoom";
import {closeExportOutput, copyExportOutput, exportLaTeX} from "./components/latexOutput";

/**
 * Sets up the global variables and initialises the panzoom instance.
 *
 * Can be called again to reset the state of the app
 */
export function initialise(skipImages: boolean = false): void {
    // console.log('initialising');
    resetInterfaceGlobals();
    resetCopyCache();
    if (!skipImages) {
        loadImages();
    }
    resetTreeManipulation();
    setUpPanZoom(getTree());

    setupButtons();
    startNodeBlank();
}

function setupButtons() {
    getUndoButton().addEventListener('click', undo);
    getRedoButton().addEventListener('click', redo);
    getPasteButton().addEventListener('click', contextMenuPaste);
    getCopyButton().addEventListener('click', contextMenuCopy);
    getDeleteButton().addEventListener('click', (e) => clearTreeNode(e));
    getZoomToFitButton().addEventListener('click', zoomToFit);
    getContextMenuZoomToFitButton().addEventListener('click', zoomToFit);
    getSaveButton().addEventListener('click', saveTree);
    getLoadButton().addEventListener('click', loadTree);
    getExportLatexButton().addEventListener('click', exportLaTeX);
    getExportCopyButton().addEventListener('click', copyExportOutput);
    getExportCloseButton().addEventListener('click', closeExportOutput);
    getToggleControlsButton().addEventListener('click', toggleControls);
}

(window as any).initialise = initialise;

(window as any).startNodeBlank = doStartNodeBlank;
// (window as any).undo = undo;
// (window as any).redo = redo;
// (window as any).zoomToFit = zoomToFit;
(window as any).handleTabPressed = handleTabPressed;
// (window as any).clearTreeNode = clearTreeNode;
// (window as any).copyTreeNode = copyTreeNode;
// (window as any).pasteTreeNode = pasteTreeNode;
(window as any).handleLiteralChanged = handleLiteralChanged;
// (window as any).saveTree = saveTree;
// (window as any).loadTree = loadTree;
// (window as any).exportLaTeX = exportLaTeX;
// (window as any).copyExportOutput = copyExportOutput;
// (window as any).closeExportOutput = closeExportOutput;
(window as any).setupExampleSelector = setupExampleSelector;
(window as any).exampleLiteralChanged = exampleLiteralChanged;
(window as any).updateTextInputWidth = updateTextInputWidth;
