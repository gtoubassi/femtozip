# FemtoZip 

FemtoZip is a "shared dictionary" compression library optimized for small documents that may not compress well with traditional tools such as gzip. In particular, situations where a very large number of small documents (10's to 1000's of bytes) share similar characteristics, but do not compress effectively standalone.  Since FemtoZip's creation, Facebook has open sourced [Zstd](https://github.com/facebook/zstd) which has support for dictionary compression and is discussed more [here](https://github.com/gtoubassi/femtozip/wiki/FemtoZip-vs-VCDiff-and-Zstd#femtozip-vs-zstd).

### How can I tell if my data will work with femtozip?

   1. If gzipping 1000 of your documents concatenated together in a single file achieves much better compression rates then individual documents, then your data is likely tailor made for FemtoZip.
   2. Get your documents onto the file system as discrete files, and run a test using the fzip command line tool as shown in the [Tutorial](https://github.com/gtoubassi/femtozip/wiki/Tutorial).
   3. If you have a Lucene search index and you want to see how much FemtoZip can compress your stored fields, try the [IndexAnalyzer](https://github.com/gtoubassi/femtozip/wiki/Indexanalyzer)

### Examples where FemtoZip is likely to outperform gzip:

   1. Small objects serialized and stored in a database or in memory DHT such as memcached using php, json, or xml serialization format. Keys and tags are repeated across documents, but may not be repeated within a document. For example in one large scale consumer website, memcached user objects (via php serialization) were compressed to 29% of their gzipped size (8.3% of their original size).  [Doblander at al](https://dl.acm.org/doi/abs/10.1145/2933267.2933308) used FemtoZip to prototype such a system.
   2. Urls, for example stored in a Lucene search index. Urls often start with "http://www.", and have common substrings like ".com/", ".html", "?page=". Again this structure is repeated across documents, but not within a document. For example in a large scale search engine urls in Lucene were compressed to 60% of their gzipped size (20% of their original size).

### Learn More

To learn more about how FemtoZip [works](https://github.com/gtoubassi/femtozip/wiki/How-femtozip-works), and how to [build](https://github.com/gtoubassi/femtozip/wiki/How-to-build) and [use](https://github.com/gtoubassi/femtozip/wiki/Tutorial) it, check out the [FemtoZip wiki](https://github.com/gtoubassi/femtozip/wiki).
