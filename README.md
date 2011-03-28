# FemtoZip

FemtoZip is a compression library in the spirit of DEFLATE (gzip) which is designed to compress very small documents (10's to 1000's of bytes) that follow a pattern. It achieves this by building a model based on sampling representative documents, and then later uses that model for compression/decompression of other documents. For illustrative purposes, two use cases where femtozip would outperform gzip:

   1. PHP or JSON serialized objects stored in memcached. The keys are repeated across documents, but not within a document.
   2. URLs stored in a free text search index such as Lucene. Urls often start with "http://www.", and have common pieces like ".com/" and ".html". Again this structure is repeated across documents, but not within a document.

For more information, visit the FemtoZip wiki at http://github.com/gtoubassi/femtozip/wiki
