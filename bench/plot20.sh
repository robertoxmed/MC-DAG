#! /usr/bin/gnuplot
set term pngcairo dashed
set output "results20.png"
set style fill solid 1.00 border 0
set xlabel "U HI" font "bold"
set ylabel "Acceptance rate (%)" font "bold"
set datafile separator ","

set style line 1 lc rgb "#FF0000" lt 1 dashtype 2
set style line 2 lc rgb "#0B610B" lt 2 dashtype 2
set style line 3 lc rgb "#610B0B" lt 3 dashtype 2
set style line 4 lc rgb "#29088A" lt 4 dashtype 2

plot "results20-2.csv" using ($1==4 ? $2:1/0):3 lw 2 title "4" with lines,\
"results20-2.csv" using ($1==7 ? $2:1/0):3 lw 2 title "7" with lines,\
"results20-2.csv" using ($1==7.5 ? $2:1/0):3 lw 2 title "7.5" with lines,\
"results20-2.csv" using ($1==8 ? $2:1/0):3 lw 2 title "8" with lines,\
"results20-2.csv" using ($1==4 ? $2:1/0):4 ls 1 lw 2 title "4 (B)" with lines,\
"results20-2.csv" using ($1==7 ? $2:1/0):4 ls 2 lw 2 title "7 (B)" with lines,\
"results20-2.csv" using ($1==7.5 ? $2:1/0):4 ls 3 lw 2 title  "7.5 (B)" with lines,\
"results20-2.csv" using ($1==8 ? $2:1/0):4 ls 4 lw 2 title "8 (B)" with lines
