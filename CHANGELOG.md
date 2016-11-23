# realm-recyclerview-lite Changelog

## 3.1.0
* Added `SelectionChangeListener` interface so that an implementer can register with a `RealmRecyclerViewAdapter` to be notified when the item selection set changes.

## 3.0.0
* Updated to support Realm 2.0.2

## 2.1.2
* Make "better logic" from 2.1.1 a bit less squirrely 🐿

## 2.1.1
* Better logic for notifying the `RecyclerView` about single item moves

## 2.1.0
* Implemented swiping functionality, see the README for details

## 2.0.5
* Removed `final` modifier from `saveInstanceState` and `restoreInstanceState` methods on `RealmRecyclerViewAdapter`

## 2.0.4
* Tested with Realm 1.0.0 and confirmed working

## 2.0.3
* Don't clip the actual `RecyclerView` to padding

## 2.0.2
* Exposed some additional `ItemTouchHelper.Callback` methods through the `RealmRecyclerViewAdapter`:
    * `onSelectedChanged`
    * `clearView`
    * `onChildDraw`
    * `onChildDrawOver`

## 2.0.1
* System scrollbars are now actually drawn if the fast scroller is disabled.
* Add attributes and methods to `RealmRecyclerView` to set padding for the real `RecyclerView` which backs it. See the [Padding](https://github.com/bkromhout/realm-recyclerview-lite#padding) section of the README for more info
* Fixed some jittery behavior that the fast scroller exhibited when the handle was at the end of its track

## 2.0.0
* The project has been refactored so that the adapter class no longer needs to be under the `io.realm` package (AKA, we no longer are using internal Realm APIs!) 🎉. This is a very large breaking change, so be sure to read over the README and look at the sample project to see what's changed if the rest of the points here aren't sufficient
* The `RealmBasedRecyclerViewAdapter` class has been removed and replaced with the `RealmRecyclerViewAdapter` class now that we no longer use internal Realm APIs. The class functions the same, except:
    * The type variable for the adapter now has the signature `<T extends RealmModel & UIDModel, ...>` instead of `<T extends RealmObject, ...>` (`UIDModel` is explained a bit further down)
    * The constructor now only takes a `Context` and `RealmResults` as its parameters.  
    The `automaticUpdate` and `animateResults` parameters have been removed since those wishing to use a `RecyclerView` which doesn't automatically update need not use this library.  
    The `animateExtraField` parameter has been removed because the same functionality can be achieved using the new `UIDModel` interface.
    * The `getLastItem` method has been removed
* Model classes which you want to display using a `RealmRecyclerViewAdapter` must implement [the `UIDModel` interface](library/src/main/java/com/bkromhout/rrvl/UIDModel.java). It contains one method, `Object getUID()`, which allows the adapter to uniquely identify each item in order to continue to support predictive animations, drag-and-drop, etc without the use of Realm's internal APIs.  
Those wishing to continue using the `animateExtraField` functionality can simply combine that field's value into whatever is returned

## 1.9.5
* Updated to work with Realm 0.90.0

## 1.9.4
* Fixed `NullPointerException` which occurred if `null` was passed to `RealmRecyclerView.setAdapter`

## 1.9.3
* Realm 0.89.1
* Fixed issue caused by changes to RealmResults in realm 0.89.0 which cause them not to be updated in a timely manner

## 1.9.2
* More Realm 0.89.0 support. Be sure that you add the `@Required` annotation to your primary key field and any fields used as extra animation fields

## 1.9.1
* Support for Realm 0.89.0. Please note the changes in the drag and drop section!
* Synced up fast scroll handle once and for all!

## 1.9.0
* Fixed CHANGELOG
* Renamed attributes
* Renamed `RealmRecyclerView.setFastScrollEnabled` to `RealmRecyclerView.setFastScroll`
* To start a drag from an adapter, you should now call `startDragging` instead of `startDragListener.startDragging`
* `RealmRecyclerView`'s drag and drop functionality can now be set programmatically using the `setDragAndDrop` and `setLongClickTriggersDrag` methods.
* Added getters to `RealmRecyclerView`

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
