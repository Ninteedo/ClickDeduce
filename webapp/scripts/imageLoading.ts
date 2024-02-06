const zoomToFit = require('../images/zoom_to_fit.svg');

export function loadImages() {
    loadZoomToFit();
}

function loadZoomToFit() {
    const button = document.querySelector('#zoom-to-fit') as HTMLButtonElement;
    const newImage = document.createElement('img');
    newImage.src = zoomToFit;
    button.appendChild(newImage);
}
