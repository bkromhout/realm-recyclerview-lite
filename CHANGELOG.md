# realm-recyclerview-lite Changelog

## 1.9.0
* Fixed CHANGELOG
* Renamed attributes, sorry for the churn!
* Renamed `RealmRecyclerView.setFastScrollEnabled` to `RealmRecyclerView.setFastScroll`
* To start a drag from an adapter, you should now call `startDragging` instead of `startDragListener.startDragging`
* `RealmRecyclerView`'s drag and drop functionality can now be set programmatically using the `setDragAndDrop` and `setLongClickTriggersDrag` methods.

## 1.8.0
* Refactored fast scroller implementation
    * Normal scroll positions vs. fast scroller positions should be much more aligned now
    * Now any class may implement the `BubbleTextProvider` interface's `getFastScrollBubbleText` method instead of it being constrained to a concrete `RealmBasedRecyclerViewAdapter` implementation. Said class should then be passed to the `RealmRecyclerView` using its `setBubbleTextProvider` method
* Added an interface, `FastScrollHandleStateListener`, which can be implemented to receive notifications about when the handle becomes shown, hidden, pressed, and released.

## 1.7.1
* Renamed `getLayoutManger()` to `getLayoutManager()` (whoops!)
* Renamed `setAutoHideFastScrollerHandle()` to `setAutoHideFastScrollHandle()`
* Finally added README and CHANGELOG files

## 1.7.0
* Added fast scroller functionality

## 1.6.1
* Added a sample application to the project
* Fixed drag-and-drop, which randomly broke itself...see [issue \#4](https://github.com/bkromhout/realm-recyclerview-lite/issues/4)

## 1.6.0
* Change API to expose RecyclerView instead of wrapping its methods

## 1.5.1
* Fixed bugs in change processing

## 1.5.0
* Renamed package and reworked structure in order to publish to jCenter() instead of using JitPack.io

## 1.4.2
* Added information needed for [AboutLibraries](https://github.com/mikepenz/AboutLibraries) to pick us up

## 1.4.1
* Fixed a bug with saving/restoring selected items

## 1.4
* Latest support libraries and gradle plugin
* Added ability to save and restore the currently selected items using `saveInstanceState()` and `restoreInstanceState()`

## 1.3
* Latest Realm version.

## 1.2
* Switched to Realm's gradle plugin
* Renamed `getNumSelected()` to `getSelectedItemCount()` to be more in line with official nomenclature

## 1.1
* Added `notifySelectedItemsChanged()` method

## 1.0
* First release of realm-recyclerview-lite!
* Added multi-select functionality
* ~~Added drag-and-drop functionality~~ Drag-and-drop *was* working correctly in this release. See notes for 1.6.1 for more info
