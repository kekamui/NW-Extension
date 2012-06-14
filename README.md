# The "New" NetLogo Network Extension

This is a new, experimental, version of the Network Extension that is currently bundled with NetLogo (see https://github.com/NetLogo/Network-Extension for the current version of the extension.)

This version of the extension is **not** pre-installed in NetLogo 5.0.1. To use it, you will need to either build it yourself ([see below](https://github.com/nicolaspayette/netlogo-network/blob/master/README.md#building)) or download it from ***PROVIDE DOWNLOAD LINK***.

(For help with extensions in general, see the NetLogo User Manual.)

This extension is at a very early stage of development.  Users are invited to experiment with it and report any issues they might find [here on GitHub](https://github.com/nicolaspayette/netlogo-network/issues?state=open), but it should not be used for production code.

The source code for the extension is currently hosted online at
https://github.com/nicolaspayette/netlogo-network.

## Changes

Compared to the current extension, this new version offers:

- **Improved performance and functionality of existing features**: pathfinding primitives are now faster and allow taking edge weights into account.
- **Centrality measures**: calculate the betweenness centrality, closeness centrality and eigenvector centrality of the nodes in your network.
- **Clusterers**: find bicomponent and weak component clusters in your network.
- **Clique finder**: find all maximal cliques or the biggest maximal clique in your network.
- **Generators**: generate many different kinds of networks, namely, preferential attachment, random, small world, 2D lattice, ring, star, and wheel networks.
- **Import/Export**: save and load your networks using plain text matrix files.

There is also more to come in the future. This is just a very preliminary version of what we have in mind. Future versions will include import/export to other formats like [GraphML](http://graphml.graphdrawing.org/) and [Pajek](http://pajek.imfm.si/doku.php), some new network layouts, and more algorithms and measures.

To provide all this functionality, the Network Extension is relying on two external, popular and well-tested network librairies: [Jung](http://jung.sourceforge.net/) and [JGraphT](https://github.com/jgrapht/jgrapht).

## Usage

The first thing that one needs to understand in order to work with the network extension is how to tell the extension _which_ network to work with. Consider the following example situation:

    breed [ bankers banker ]
    breed [ clients client ]
    
    undirected-link-breed [ friendships friendship ]
    directed-link-breed [ accounts account ]

Basically, you have bankers and clients. Clients can have accounts with bankers. Bankers can probably have account with other bankers, and anyone can be friends with anyone.

Now it is possible that you want to consider this whole thing as one big network, but it seems more likely that you will only be interested in a subset of it. Maybe you want to consider all friendships, but you might also want to consider only the friendship between bankers. After all, something having a very high centrality in the network of banker friendship is very different from having a high centrality in a network of client frienships.

To specify such networks, we need to tell the extension _both_ which turtles _and_ which links we are interested in. All the turtles from the specified set of turtles will be included in the network, and only the links from the specified set of links that are between turtles of the specified set will be included. For example, if you ask for `bankers` and `friendships`, even the lonely bankers with no friends will be included, but friendship links between bankers and clients will **not** be included. The current way to tell the extension about this is with the `nw:set-snapshot` primitive, which you must call _prior_ to doing any operations on a network.

Some examples:

- `nw:set-snapshot turtles links` will give you everything: bankers and clients, frienships and accounts, as one big network.
- `nw:set-snapshot turtles friendships` will give you all the bankers and clients and friendships between any of them.
- `nw:set-snapshot bankers friendships` will give you all the bankers, and only friendships between bankers.
- `nw:set-snapshot bankers links` will give you all the bankers, and any links between them, whether these links are friendships or accounts.
- `nw:set-snapshot clients accounts` will give all the clients and accounts between each other, but since in our fictionnal example clients can only have accounts with bankers, this will be a completely disconnected network.

Now one very important thing that you need to understand about `set-snapshot` is that, as its name suggests, it takes a static picture of the network at the time you call it. All subsequent network operations will use this static picture, _even if turtles or links have been created or died in the meantime_, until you call `set-snapshot` again.

In pratice, this means that you will write code like:

    nw:set-snapshot bankers friendships
    ask bankers [
      set size nw:closeness-centrality
    ]

This also means that you need to be careful:

    nw:set-snapshot bankers friendships
    create-bankers 1                    ; creates a new banker after taking the snapshot
    show nw:mean-path-length            ; this is OK, it just won't take the new banker into account
    ask bankers [
      set size nw:closeness-centrality  ; THIS WILL FAIL FOR THE NEWLY CREATED BANKER
    ]

In the example above, a banker is created _after_ the snapshot is taken. This is not a problem in itself: you can still run some measures on the network, such as `nw:mean-link-path-length` in the example above, but if you try to ask the newly created banker for, e.g., its closeness centrality, the extension will give you a runtime error.

One reason why things work the way they do is that it allows the extension to _cache_ the result of some computations. Many network algorithms are designed to operate on the whole network at once. In the example above, the closeness centrality is actually calculated for every banker the first time you ask for it and then stored in the snapshot so that other bankers just have to access the result.

This makes a big difference, in particular, for primitives like `nw:link-distance`, which uses [Dijkstra's algorithm](http://en.wikipedia.org/wiki/Dijkstra%27s_algorithm). Without getting into the details of the algorithm, let's just say that a big part of the calculations that are made in finding the shortest path from `turtle 0` to `turtle 10` can be reused when finding the shortest path from `turtle 0` to `turtle 20`, and that these calculations are stored in the snapshot.

### Future Usage

Now wouldn't it be better if you _didn't_ have to call `nw:set-snapshot` everytime you want to do something with a network? Yes, indeed, it would. And eventually, it will be the case. What we have in mind for the moment is something like a `nw:set-context` primitive, which you would use to tell the extension that "in general, these are the turtles and links I want to work with." Once you set the context, the extension will be wise enough to decide by itself if it needs to take a new snapshot or not.

The reason we did not do it like this right away is that there currently is no efficient way to ask NetLogo if turtles and links have been created or deleted since a previous function call. If we can include this functionality in a future version of NetLogo, we will probably deprecate `nw:set-snapshot` and provide the much more convenient `nw:set-context` instead.

## Primitives

### network:in-link-radius, network:in-out-link-radius, network:in-in-link-radius

![turtle](https://github.com/NetLogo/Network-Extension/raw/master/turtle.gif) `TURTLESET network:in-link-radius RADIUS LINK-BREED`  
![turtle](https://github.com/NetLogo/Network-Extension/raw/master/turtle.gif) `TURTLESET network:in-out-link-radius RADIUS LINK-BREED`  
![turtle](https://github.com/NetLogo/Network-Extension/raw/master/turtle.gif) `TURTLESET network:in-in-link-radius RADIUS LINK-BREED`

example: `ask one-of bankers [ show other bankers network:in-link-radius 5 friendships ]`

Returns the set of turtles within the given distance (number of links followed)
of the calling turtle.
Searches breadth-first from the calling turtle,
following links of the given link breed.

The `in-link-radius` form works with undirected links.  The other two
forms work with directed links; `out` or `in` specifies whether links
are followed in the normal direction (`out`), or in reverse (`in`).

### network:link-distance

![turtle](https://github.com/NetLogo/Network-Extension/raw/master/turtle.gif) `network:link-distance TURTLE LINK-BREED`

example: `ask one-of-bankers [ show network:link-distance the-best-banker friendships ]`

Finds the distance to the destination turtle (number of links followed).
Searches breadth-first from the calling turtle,
following links of the given link breed.

Reports false if no path exists.

### network:link-path, link-path-turtles

![turtle](https://github.com/NetLogo/Network-Extension/raw/master/turtle.gif) `network:link-path TURTLE LINK-BREED`  
![turtle](https://github.com/NetLogo/Network-Extension/raw/master/turtle.gif) `network:link-path-turtles TURTLE LINK-BREED`

example: `ask banker1 [ show network:link-path banker3 friendships ]`
->   [(link 1 2) (link 2 3)]

example:`ask banker1 [ show network:link-path-turtles banker3 friendships ]`
->   [(banker 1) (banker 2) (banker 3)]
 
Reports a list of turtles or links following the shortest path from the calling
turtle to the destination turtle.

Reports an empty list if no path exists.

If `network:link-path-turtles` is used, the calling turtle and the
destination are included in the list.

Searches breadth-first from the calling turtle,
following links of the given link breed.

Follows links at the same depth in random order.  If there are
multiple shortest paths, a different path may be returned on
subsequent calls, depending on the random choices made during search.

### network:mean-link-path-length

`network:mean-link-path-length TURTLE-SET LINK-BREED`

Reports the average shortest-path length between all distinct pairs of
nodes in the given set of turtles, following links of the given link
breed.

Reports false unless paths exist between all pairs.

### `nw:closeness-centrality`

- intra-component closeness
- reports 0 for isolates

## Transition guide



## Building

The extension is written in Scala (version 2.9.1).

Unless you are compiling the extension from a `extensions/nw` under a NetLogo source code distribution, you will need to use the `NETLOGO` environment variable to point to your NetLogo directory (containing `NetLogo.jar`) and the `SCALA_HOME` variable to point to your Scala 2.9.1 installation. For example:

    NETLOGO=/Applications/NetLogo\\\ 5.0.1 SCALA_HOME=/usr/local/scala-2.9.1.final make

Unless they are already present, the Makefile will download the needed Jung and JGraphT jar files from the Internet. Note that the extension requires a modified version of `jung-algorithms-2.0.2` (namely, `jung-algorithms-2.0.2-nlfork-0.1.jar`) which will be downloaded from the CCL server.

If compilation succeeds, `nw.jar` will be created. To use the extension, this file and all the other jars will need to be in the `extensions/nw` folder under your NetLogo installation.

## Credits

The first versions of the network primitives were written by Forrest Stonedahl. They were then ported to a NetLogo 5.0 extension by Seth Tisue.

## Terms of Use

[![CC0](http://i.creativecommons.org/p/zero/1.0/88x31.png)](http://creativecommons.org/publicdomain/zero/1.0/)

The NetLogo network extension is in the public domain.  To the extent possible under law, Uri Wilensky has waived all copyright and related or neighboring rights.
