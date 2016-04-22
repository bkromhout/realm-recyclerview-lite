# Ordering Scheme Notes

One of the complexities involved in keeping track of an order for any set of items is how you update an item's position when it moves. When you move an item to any position other than the first or last, its new position must be somewhere between those of the items on either side of its new place.

Since my position variable is an `int`, I can't just divide it. It is for this reason that in my ordering scheme the items initially have 100-space gaps between them. This large gap means that most of the time a move operation will only require us to calculate the median of the positions of the items on either side, something which can be done in "near-constant" time.

Of course, if a user is doing lots of reordering, they might get into a situation where they've used up that gap somewhere and truly have no room left between the two items they wish to relocate another item between. In my implementation I handle this by taking the time to re-order *the whole list* before trying to find that median position again.

Before writing this off as a bad idea, consider how fast Realm is; the delay is not significant unless you have a *very* large number of items to iterate through.

Plus, you could easily set up a job which periodically runs at non-peak times and does this for you. If you set up such a job to run at midnight each night, and choose a good initial gap, then you'll end up with a fairly foolproof ordering implementation whose re-ordering operations run in constant time 99% of the time.
