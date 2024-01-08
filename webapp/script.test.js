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

jest.mock('panzoom', () => {
    // Mock the panzoom function
    return jest.fn().mockImplementation(() => {
        // Return an object that resembles the panzoom instance
        return {
            zoomAbs: jest.fn(),
            moveTo: jest.fn(),
            // Add more methods as needed for your tests
        };
    });
});

const script = require("./script.js");
