#! /usr/bin/gnuplot
set term pngcairo dashed
set output "results20.png"
set border 3
set xlabel "U HI" font "bold"
set ylabel "Acceptance rate (%)" font "bold"
set datafile separator ","

set style line 1 lc rgb "#FF0000" lt 1 lw 2 pt 1 pi -1 ps 1 dashtype 3
set style line 2 lc rgb "#0B610B" lt 2 lw 2 pt 2 pi -1 ps 1 dashtype 3
set style line 3 lc rgb "#610B0B" lt 3 lw 2 pt 3 pi -1 ps 1 dashtype 3
set style line 4 lc rgb "#29088A" lt 4 lw 2 pt 4 pi -1 ps 1 dashtype 3

plot "results20-2.csv" using ($2==4 ? $3:1/0):6 lw 2 title "4" with linespoints,\
"results20-2.csv" using ($2==7 ? $3:1/0):6 lw 2 title "7" with linespoints,\
"results20-2.csv" using ($2==7.5 ? $3:1/0):6 lw 2 title "7.5" with linespoints,\
"results20-2.csv" using ($2==8 ? $3:1/0):6 lw 2 title "8" with linespoints,\
"results20-2.csv" using ($2==4 ? $3:1/0):7 title "4 (B)" with linespoints ls 1,\
"results20-2.csv" using ($2==7 ? $3:1/0):7 title "7 (B)" with linespoints ls 2,\
"results20-2.csv" using ($2==7.5 ? $3:1/0):7 title "7.5 (B)" with linespoints ls 3,\
"results20-2.csv" using ($2==8 ? $3:1/0):7 title "8 (B)" with linespoints ls 4
