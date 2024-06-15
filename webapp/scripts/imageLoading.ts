// @ts-ignore
import ZoomToFitSvg from '/webapp/images/zoom_to_fit.svg';

export function loadImages() {
    loadZoomToFit();
}

function loadZoomToFit() {
    const button = document.querySelector('#zoom-to-fit') as HTMLButtonElement;
    const newImage = document.createElement('img');
    newImage.src = ZoomToFitSvg;
    button.appendChild(newImage);
}
