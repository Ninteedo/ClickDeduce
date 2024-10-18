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
import {getTree, loadTree, redo, resetTreeManipulation, saveTree, undo, updateTextInputWidth} from "./treeManipulation";
import {
    closeExportOutput,
    copyExportOutput,
    exportLaTeX,
    getContextMenuZoomToFitButton,
    getCopyButton,
    getDeleteButton,
    getExportCloseButton,
    getExportCopyButton,
    getExportLatexButton,
    getLoadButton,
    getPasteButton,
    getSaveButton,
    getZoomToFitButton,
    handleTabPressed,
    resetInterfaceGlobals,
    setPanZoomInstance,
    zoomToFit
} from "./interface";
import panzoom, {PanZoom} from "panzoom";
import {loadImages} from "./imageLoading";
import {setupExampleSelector} from "./customExprSelector";
import {getRedoButton, getUndoButton} from "./test/helper";

export let panzoomInstance: PanZoom;

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
    setPanZoomInstance(panzoom(getTree(), {
        bounds: true,
        boundsPadding: -0.1,
        zoomDoubleClickSpeed: 1,
        minZoom: 0.1,
        maxZoom: 10,
        onTouch: () => {
            // TODO: cannot use on mobile currently
            return false;  // tells the library to not preventDefault.
        },
        filterKey: () => {
            return true;  // don't let panzoom handle this event:
        }
    }));

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
