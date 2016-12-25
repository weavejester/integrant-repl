# Integrant-REPL

A Clojure library that implements the user functions of Stuart Sierra's
reloaded workflow for [Integrant][].

It's very similar to [reloaded.repl][], except that it works for
Integrant, rather than [Component][].

[integrant]: https://github.com/weavejester/integrant
[reloaded.repl]: https://github.com/weavejester/reloaded.repl
[component]: https://github.com/stuartsierra/component

## Install

Add the following dependency to your dev profile:

    [integrant/repl "0.1.0-SNAPSHOT"]

## Usage

Require the `integrant.repl` namespace in your user.clj file, and use
the set-prep! function to define a zero-argument function that returns
an Integrant configuration.

For example:

```clojure
(ns user
  (:require [integrant.repl :refer [config system prep init go halt
                                    clear reset reset-all]])

(set-prep! (constantly {::foo {:example? true}}))
```

## License

Copyright © 2016 James Reeves

Released under the MIT license.
