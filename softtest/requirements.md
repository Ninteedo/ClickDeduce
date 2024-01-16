# Requirements
1. The system will allow the user to intuitively view an evaluation tree
   1. The user will be able to click and drag the tree to pan around it
   2. The user will be able to scroll to zoom in and out
   3. The subtree that the user is hovering over will be highlighted
   4. The evaluation tree will be displayed as...
2. The system will allow the user to construct an evaluation tree from scratch
   1. The user will be able to create a new evaluation tree with only a root node
   2. The user will be able to select an evaluation rule available in the language
      1. Each empty node will have a dropdown menu of available evaluation rules
   3. Evaluation rules can have any number of subexpressions
      1. Evaluation rules without any subexpressions are axioms, and the end of the subtree
      2. Each subexpression has its own subtree
   4. The user will be able to select a subexpression of the current node to be the next node
   5. The user will be able to enter text where literals occur
      1. While literals may have specific syntax rules, the user will be able to enter any text
         1. Invalid literals will result in an evaluation error being displayed in the tree value
3. The system will allow the user to edit an existing evaluation tree
   1. The user will be able to right-click on a node to open a context menu, focused on that node
      1. The highlighted subtree will be the one focused on when the context menu is opened
      2. The focused subtree will remain highlighted as long as the context menu is open
      3. Operations from the context menu will target the focused subtree
   2. The user will be able to delete a node, resetting that subtree to an empty state
   3. The user will be able to copy a node, and paste it somewhere else in the tree
      1. It is only possible to paste a subtree after copying a subtree
      2. Any subtree can be copied, including the root node
   4. The user will be able to reset the entire tree to an empty state
4. The user will be able to use one of multiple evaluation languages
   1. The user will be able to select a language from a dropdown menu
      1. The server will provide a list of available languages
      2. The server will provide a list of available evaluation rules for each language
   2. The user can switch which language is being used at any time, as long as it is valid
      1. The server will validate the tree when the user switches languages
         1. The server will check that the tree does not contain any evaluation rules or expressions which do not exist in the new language
      2. The user will then be able to use evaluation rules from the new language
      3. The language will be switched whenever the user changes the language dropdown value