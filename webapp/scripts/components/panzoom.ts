import {PanZoom} from "panzoom";
import {getTreeContainer} from "../globals/elements";

let panzoomInstance: PanZoom;

/**
 * Zooms the tree to fit the container.
 */
export async function zoomToFit(): Promise<void> {
    const container: HTMLElement = getTreeContainer();
    const rootSubtree: HTMLDivElement = document.querySelector('.subtree[data-tree-path=""]') as HTMLDivElement;

    const scaleWidth = container.clientWidth / rootSubtree.clientWidth;

    panzoomInstance.moveTo(0, 0);
    panzoomInstance.zoomAbs(0, 0, scaleWidth);
}

/**
 * Returns whether the auto-zoom checkbox is checked.
 */
export function isAutoZoomEnabled(): boolean {
    const autoZoomCheckbox = document.getElementById('auto-zoom-toggle') as HTMLInputElement;
    return autoZoomCheckbox.checked;
}

export function setPanZoomInstance(instance: PanZoom): void {
    panzoomInstance = instance;
}
