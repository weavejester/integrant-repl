# Integrant-REPL

A Clojure library that implements the user functions of [Stuart Sierra's
reloaded workflow](https://cognitect.com/blog/2013/06/04/clojure-workflow-reloaded) for [Integrant][].

It's very similar to [reloaded.repl][], except that it works for
Integrant, rather than [Component][].

[integrant]: https://github.com/weavejester/integrant
[reloaded.repl]: https://github.com/weavejester/reloaded.repl
[component]: https://github.com/stuartsierra/component

## Install

Add the following dependency to your dev profile:

    [integrant/repl "0.3.2"]

## Usage

Require the `integrant.repl` namespace in your user.clj file, and use
the set-prep! function to define a zero-argument function that returns
an Integrant configuration.

For example:

```clojure
(ns user
  (:require [integrant.repl :refer [clear go halt prep init reset reset-all]]))

(integrant.repl/set-prep! (constantly {::foo {:example? true}}))
```

To prepare the configuration, you can now use:

```clojure
user=> (prep)
:prepped
```

The configuration is stored in `integrant.repl.state/config`. To
initiate the configuration, use:

```clojure
user=> (init)
:initiated
```

This will turn the configuration into a running system, which is
stored in `integrant.repl.state/system`.

Because these two steps are common, we can instead use:

```clojure
user=> (go)
:initiated
```

This performs the `(prep)` and `(init)` steps together. Once the
system is running, we can stop it at any time:

```clojure
user=> (halt)
:halted
```

If we want to reload our source files and restart the system, we can
use:

```clojure
user=> (reset)
:reloading (...)
:resumed
```

Behind the scenes, Integrant-REPL uses [tools.namespace][]. You can
set the directories that are monitored for changed files by using the
`refresh-dirs` function:

```clojure
user=> (require '[clojure.tools.namespace.repl :refer [set-refresh-dirs]])
nil
user=> (set-refresh-dirs "src/clj")
("src/clj")
```

[tools.namespace]: https://github.com/clojure/tools.namespace/


## License

Copyright © 2020 James Reeves

Released under the MIT license.
