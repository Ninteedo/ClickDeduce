export function loadImages() {
    loadZoomToFit();
}

function loadZoomToFit() {
    const button = document.querySelector('#zoom-to-fit') as HTMLButtonElement;
    const newImage = document.createElement('img');
    newImage.src = require('../images/zoom_to_fit.svg');
    button.appendChild(newImage);
}
