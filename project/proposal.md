**What will you do?**

We will work on [AIRduct](https://squera.github.io/fcs-pdf/dingFCS2023.pdf), the array-based IR for the [Viaduct](https://www.cs.cornell.edu/andru/papers/viaduct/viaduct-tr.pdf) compiler.
This is based on a research project that Vivian has been working on for the last couple of years.

TODO: Regurgitate info about Viaduct and AIRduct from the papers.
    For Viaduct, the normal schpiel.
    For AIRduct, why circuits, why arrays, and why big circuits/grouping

We are focusing on implementing the following:

- Supporting control flow
- A grouping procedure for creating circuit blocks (in the IR) from programs where each statement is annotated with
a protocol. This will include reordering of code.
- Array optimizations, such as vectorization

**How will you do it?**

- Control flow should hopefully be straightforward.
- Grouping requires analysis regarding which reorderings are safe, with respect to both data dependencies and security.
- Optimizations will be non-trivial but will be inspired on prior work.
    - One optimization is simply calling into SIMD instructions which are natively supported by many cryptographic
    libraries.
    - A more interesting optimization is the combination of bulk instructions. An example is merging two array map expressions into one, when the
    second map operates on the output of the first one (similar to loop fusion).

**How will you empirically measure success?**

- We will measure success of implementing control flow checking correctness on the already existing tests cases.
- We are not confident how to measure success of grouping yet. A good metric for the quality of a grouping might be the
resulting number of blocks, but it's not obvious whether one can compute the minimal number of blocks to compare our
implementation to. There doesn't exist a unique maximal grouping, but there definitely exists some set of maximal
groupings for any given program.
- The input code will be manually written test cases. We will try to write programs which use each implemented
feature, such as taking different paths through the CFG and taking advantage of vectorization. We will also include
benchmark programs, which were used by the original Viaduct paper and are also often used to evaluate other
cryptographic compilers.
- We will test for correctness by ensuring that output is the same with and without calling into cryptographic
protocols, that is, a computation using cryptography should yield the same result as in cleartext; the only difference
being the added security properties.
- We can measure the benefit of optimizations by examining run time.

**Team members:**
[Vivian Ding](https://github.com/vivianyyd)
[William Wang](https://github.com/willwng)

TODO Vivian: Read my notebook notes and regurgitate more information here