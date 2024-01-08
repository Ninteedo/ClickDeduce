document.body.innerHTML = `
  <button onclick="undo()" id="undoButton">Undo</button>
  <button onclick="redo()" id="redoButton">Redo</button>
  <div id="tree-container">
    <div id="tree-buttons">
      <button onclick="zoomToFit()">
        <img src="images/zoom_to_fit.svg" alt="Zoom to Fit">
      </button>
    </div>
    <div id="tree"></div>
  </div>
`;

const script = require("./script.js");
