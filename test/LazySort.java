
class Lazysort  {
    public static void main(String[] a) {
        int[] L;
        int i;
        boolean d;
        Lazysort q;

        L = new int[10];

        i = 0;
        while (i < L.length) {
            L[i] = L.length - i;
            i = i + 1;
        }

        q = new Lazysort();

        q.quicksort(L);
    }

    public boolean quicksort(int[] L) {
        boolean lazy;
        int rand = 2;
        if (rand < 4 ) {
            this.beLazy(L);
            lazy = true;
        }
        else {
            lazy = false;
        }

        if ( lazy ) {
            lazy = !lazy;
        }
        else { }

        return lazy;
    }

    public boolean beLazy(int[] L) {
        int _allowedNameL;
        int _allowedNameI;
        int rand = 3;
        _allowedNameL = L.length;

        _allowedNameI = 0;
        while (_allowedNameI < _allowedNameL/2) {
            _allowedNameI = _allowedNameI + 1;
        }

        while (_allowedNameI < _allowedNameL) {
            L[_allowedNameI] = rand + 1;

            _allowedNameI = _allowedNameI + 1;
        }


        return true;
    }
}
