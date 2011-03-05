/**
 *   Copyright 2011 Garrick Toubassi
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *   
 *   This file is a Java port of two files found at http://www.cs.dartmouth.edu/~doug/sarray/
 *   
 *   sarray.c: suffixarray generation, written by Sean Quinlan and Sean Doward
 *   lcp.c: lcp computation implemented by Douglass McIlroy, method by Kasai et al.
 *   See comments in sarray.c and lcp.c for more details.
 */
package org.toubassi.femtozip.dictionary;

import java.io.PrintStream;
import java.util.Arrays;

public class SuffixArray {

    private static final int BUCK = Integer.MIN_VALUE;

    private static final int succ(int i, int h, int n) {
        int t = i + h;
        return t >= n ? t - n : t;
    }
        
    public static int[] computeSuffixArray(byte[] bytes) {
        byte buf[] = bytes;
        int n = bytes.length;
        int p[] = new int[n + 1];
        
        int[] a, buckets = new int[256*256];
        int i, last, cum, c, cc, ncc, lab, nbuck;
        
        a = new int[n + 1];
        
        Arrays.fill(buckets, -1);
        c = (((int)buf[n - 1]) & 0xff) << 8;
        last = c;
        for (i = n - 2; i >= 0; i--) {
            c = (((int)buf[i]) & 0xff) << 8 | (c >> 8);
            a[i] = buckets[c];
            buckets[c] = i;
        }
        
        a[n] = 0;
        
        lab = 1;
        cum = 1;
        i = 0;
        nbuck = 0;
        for (c = 0; c < 256*256; c++) {
            if (c == last) {
                a[n-1] = lab;
                cum++;
                lab++;
            }
            
            for(cc = buckets[c]; cc != -1; cc = ncc) {
                ncc = a[cc];
                a[cc] = lab;
                cum++;
                p[i++] = cc;
            }
            if(lab == cum)
                continue;
            if(lab + 1 == cum)
                i--;
            else {
                p[i - 1] |= BUCK;
                nbuck++;
            }
            lab = cum;
        }
        
        ssortit(a, p, n + 1, 2, i, nbuck);
        return p;
    }
    
    private static final int ssortit(int a[], int p[], int n, int h, int pe, int nbuck) {
        
        int s, ss, packing, sorting;
        int v, sv, vv, packed, lab, i, pi;
        
        for(pi = 0; h < n && pi < pe; h=2*h) {
            packing = 0;
            nbuck = 0;

            for(sorting = 0; sorting < pe; sorting = s){
                /*
                 * find length of stuff to sort
                 */
                lab = a[p[sorting]];
                for(s = sorting; ; s++) {
                    sv = p[s];
                    v = a[succ(sv & ~BUCK, h, n)];
                    if((v & BUCK) != 0)
                        v = lab;
                    a[sv & ~BUCK] = v | BUCK;
                    if((sv & BUCK) != 0)
                        break;
                }
                p[s++] &= ~BUCK;
                nbuck++;
                qsort2(p, sorting, a, s - sorting);
                v = a[p[sorting]];
                a[p[sorting]] = lab;
                packed = 0;
                for(ss = sorting + 1; ss < s; ss++) {
                    sv = p[ss];
                    vv = a[sv];
                    if(vv == v) {
                        p[packing++] = p[ss - 1];
                        packed++;
                    } else {
                        if(packed != 0) {
                            p[packing++] = p[ss - 1] | BUCK;
                        }
                        lab += packed + 1;
                        packed = 0;
                        v = vv;
                    }
                    a[sv] = lab;
                }
                if(packed != 0) {
                    p[packing++] = p[ss - 1] | BUCK;
                }
            }
            pe = packing;
        }

        /*
         * reconstruct the permutation matrix
         * return index of the entire string
         */
        v = a[0];
        for(i = 0; i < n; i++) {
            p[a[i]] = i;
        }
        
        return v;
    }
    
    private static final void swap2(int[] a, int ai, int[] b, int bi) {
        int t = a[ai];
        a[ai] = b[bi];
        b[bi] = t;
    }

    private static final void vecswap2(int[] a, int ai, int bi, int n)
    {
        while (n-- > 0) {
            int t = a[ai];
            a[ai++] = a[bi];
            a[bi++] = t;
        }
    }

    private static final int med3(int a[], int ai, int bi, int ci, int asucc[])
    {
        int va, vb, vc;

        if ((va=asucc[a[ai]]) == (vb=asucc[a[bi]]))
            return ai;
        if ((vc=asucc[a[ci]]) == va || vc == vb)
            return ci;
        return va < vb ?
              (vb < vc ? bi : (va < vc ? ci : ai))
            : (vb > vc ? bi : (va < vc ? ai : ci));
    }
    
    private static final void inssort(int a[], int ai, int asucc[], int n)
    {
        int pi, pj;
        for (pi = ai + 1; --n > 0; pi++)
            for (pj = pi; pj > ai; pj--) {
                if(asucc[a[pj - 1]] <= asucc[a[pj]])
                    break;
                swap2(a, pj, a, pj-1);
            }
    }

    private static final void qsort2(int a[], int ai, int asucc[], int n)
    {
        int d, r, partval;
        int pa, pb, pc, pd, pl, pm, pn;

        if (n < 15) {
            inssort(a, ai, asucc, n);
            return;
        }
        pl = ai;
        pm = ai + (n >> 1);
        pn = ai + (n - 1);
        if (n > 30) { /* On big arrays, pseudomedian of 9 */
            d = (n >> 3);
            pl = med3(a, pl, pl+d, pl+2*d, asucc);
            pm = med3(a, pm-d, pm, pm+d, asucc);
            pn = med3(a, pn-2*d, pn-d, pn, asucc);
        }
        pm = med3(a, pl, pm, pn, asucc);
        swap2(a, ai, a, pm);
        partval = asucc[a[ai]];
        pa = pb = ai + 1;
        pc = pd = ai + n - 1;
        for (;;) {
            while (pb <= pc && (r = asucc[a[pb]]-partval) <= 0) {
                if (r == 0) {
                    swap2(a, pa, a, pb);
                    pa++;
                }
                pb++;
            }
            while (pb <= pc && (r = asucc[a[pc]]-partval) >= 0) {
                if (r == 0) {
                    swap2(a, pc, a, pd);
                    pd--;
                }
                pc--;
            }
            if (pb > pc)
                break;
            swap2(a, pb, a, pc);
            pb++;
            pc--;
        }
        pn = ai + n;
        r = pa - ai;
        if(pb-pa < r)
            r = pb-pa;
        vecswap2(a, ai, pb - r, r);
        r = pn-pd-1;
        if(pd-pc < r)
            r = pd-pc;
        vecswap2(a, pb, pn-r, r);
        if ((r = pb-pa) > 1)
            qsort2(a, ai, asucc, r);
        if ((r = pd-pc) > 1)
            qsort2(a, ai + n - r, asucc, r);
    }
    
    public static int[] computeLCP(byte[] bytes, int[] suffixArray) {
        int[] a = suffixArray;
        byte[] s = bytes;
        int n = suffixArray.length;
        int[] lcp = new int[n];
        
        int i, h;
        int[] inv = new int[n];
        
        for (i = 0; i < n; i++) {
            inv[a[i]] = i;
        }
        
        h = 0;
        for (i = 0; i < n - 1; i++) {
            int x = inv[i];
            int j = a[x - 1];
            int p1 = i + h;
            int p0 = j + h;
            while (p1 < (n-1) && p0 < (n-1) && s[p1++] == s[p0++]) {
                h++;
            }
            lcp[x] = h;
            if (h > 0) {
                h--;
            }
        }
        
        lcp[0] = 0;
        return lcp;
    }
         
    /**
     * For debugging
     */
    public static void dump(PrintStream out, byte[] bytes, int[] suffixArray, int[] lcp) {
        int[] p = suffixArray;
        int n = p.length;
        
        for (int i = 0; i < n; i++) {
            String lcpString = "";
            if (lcp != null) {
                lcpString = Integer.toString(lcp[i]) + "\t";
            }
            out.print(suffixArray[i] + "\t" + lcpString);
            out.write(bytes, suffixArray[i], Math.min(40, n - 1 - suffixArray[i]));
            out.println();
        }
    }
}
