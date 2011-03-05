#ifdef __cplusplus
extern "C" {
#endif

typedef unsigned char uchar;

int ssarray(int *a);
int sarray(int *a, int n);
int bsarray(const uchar *b, int *a, int n);
int *lcp(const int *a, const char *s, int n);
int lcpa(const int *a, const char *s, int *b, int n);
int *scode(const char *s);
uchar *codetab(const uchar *s);
uchar *inverse(const uchar *t);


#ifdef __cplusplus
}
#endif
