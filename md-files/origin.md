# Origin
The **realm-recyclerview-lite** project started life as a fork of Thorben Primke's [realm-recyclerview][RRV] library; I wanted to add a feature which I then hoped to be able to have merged back in via pull request. However, this plan quickly fell apart as it became clear that my efforts were akin to shoving a square peg (or two) into a round hole.

Much of realm-recyclerview's code supports features which I don't have any intention of using, but due to how tightly coupled everything has to be to build a Realm RecyclerView, changes to any part of it meant changes to three or four other parts.  
It quickly became apparent that to implement what I wanted in a well-thought-out way would require changing most of realm-recyclerview's code. That wasn't something which I felt comfortable doing if it were to remain under the realm-recyclerview name; both because it felt disrespectful to the original, and because in truth my original goal was just to have something as small as possible while still fulfilling my needs.

After some thinking, I made the decision to rename the library, and I proceeded to remove the features I didn't need or want in order to have a cleaner base for implementing those I did. What remained after this gutting was:
* The Android view code necessary to make the custom view work
* A shell class which I used to implement drag-and-drop based on [this article][DnD] (Thorben used the class to to implement the other half of what that article describes, which is swipe-to-dismiss functionality)
* A bit of the original Realm-related code
* And last, but certainly not least, Thorben's incredibly genius concept of using java-diff-utils to  decide which of the `notify*Changed()` methods to call when a RealmChangeListener got fired

While the code for that last item has been majorly reworked to tuned it my needs in realm-recyclerview-lite, I would be remiss to not give credit to Thorben for it; such a solution never would have occurred to me. It solves a *very* tricky problem extremely elegantly, and I'm thankful to have had such a good base to build on.

[RRV]: https://github.com/thorbenprimke/realm-recyclerview
[DnD]: https://medium.com/@ipaulpro/drag-and-swipe-with-recyclerview-b9456d2b1aaf#.y5o1j11jt
