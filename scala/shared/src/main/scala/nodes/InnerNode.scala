package nodes

import convertors.DisplayMode
import languages.terms.Term

/** Parent class for nodes that do not appear on their own in the tree structure.
 */
abstract class InnerNode extends Node {

  /** Create a term placeholder for this node.
   * @param mode
   *   The display mode.
   * @param readOnly
   *   Whether the placeholder should be read-only.
   * @return
   *   The term placeholder.
   */
  def getPlaceholder(mode: DisplayMode, readOnly: Boolean = true): Term
}

