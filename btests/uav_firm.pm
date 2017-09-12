dtmc

const int D;

module Avoidfirm
	v: [0..16] init 15;
	[Avoid_end_ok] v = 0 -> (v' = 1);
	[Avoid_end_fail] v = 0 -> (v' = 0);
	[Avoid_end_ok] v = 1 -> (v' = 3);
	[Avoid_end_fail] v = 1 -> (v' = 2);
	[Avoid_end_ok] v = 2 -> (v' = 5);
	[Avoid_end_fail] v = 2 -> (v' = 4);
	[Avoid_end_ok] v = 3 -> (v' = 7);
	[Avoid_end_fail] v = 3 -> (v' = 6);
	[Avoid_end_ok] v = 4 -> (v' = 9);
	[Avoid_end_fail] v = 4 -> (v' = 8);
	[Avoid_end_ok] v = 5 -> (v' = 11);
	[Avoid_end_fail] v = 5 -> (v' = 10);
	[Avoid_end_ok] v = 6 -> (v' = 13);
	[Avoid_end_fail] v = 6 -> (v' = 12);
	[Avoid_end_ok] v = 7 -> (v' = 15);
	[Avoid_end_fail] v = 7 -> (v' = 14);
	[Avoid_end_ok] v = 8 -> (v' = 1);
	[Avoid_end_fail] v = 8 -> (v' = 0);
	[Avoid_end_ok] v = 9 -> (v' = 3);
	[Avoid_end_fail] v = 9 -> (v' = 2);
	[Avoid_end_ok] v = 10 -> (v' = 5);
	[Avoid_end_fail] v = 10 -> (v' = 4);
	[Avoid_end_ok] v = 11 -> (v' = 7);
	[Avoid_end_fail] v = 11 -> (v' = 6);
	[Avoid_end_ok] v = 12 -> (v' = 9);
	[Avoid_end_fail] v = 12 -> (v' = 8);
	[Avoid_end_ok] v = 13 -> (v' = 11);
	[Avoid_end_fail] v = 13 -> (v' = 10);
	[Avoid_end_ok] v = 14 -> (v' = 13);
	[Avoid_end_fail] v = 14 -> (v' = 12);
	[Avoid_end_ok] v = 15 -> (v' = 15);
	[Avoid_end_fail] v = 15 -> (v' = 14);

	[Avoid_fail] v = 0 -> (v' = 0);
	[Avoid_fail] v = 1 -> (v' = 1);
	[Avoid_fail] v = 2 -> (v' = 2);
	[Avoid_ok] v = 3 -> (v' = 3);
	[Avoid_fail] v = 4 -> (v' = 4);
	[Avoid_ok] v = 5 -> (v' = 5);
	[Avoid_ok] v = 6 -> (v' = 6);
	[Avoid_ok] v = 7 -> (v' = 7);
	[Avoid_fail] v = 8 -> (v' = 8);
	[Avoid_ok] v = 9 -> (v' = 9);
	[Avoid_ok] v = 10 -> (v' = 10);
	[Avoid_ok] v = 11 -> (v' = 11);
	[Avoid_ok] v = 12 -> (v' = 12);
	[Avoid_ok] v = 13 -> (v' = 13);
	[Avoid_ok] v = 14 -> (v' = 14);
	[Avoid_ok] v = 15 -> (v' = 15);
endmodule

module Avoid
	t: [0..2] init 0;
	[Avoid0_run] t = 0 ->  1 - 0.01 : (t' = 1) + 0.01 : (t' = 2);
	[Avoid_end_ok] t = 1 -> (t' = 0);
	[Avoid_end_fail] t = 2 -> (t' = 0);
endmodule

formula Video = Videobool;
formula Shar = Sharbool & Logbool & Avoidbool;
formula Rec = Recbool & GPSbool;

module proc
	s : [0..13] init 0;
	Logbool: bool init false;
	Sharbool: bool init false;
	GPSbool: bool init false;
	Recbool: bool init false;
	Avoidbool: bool init false;
	Videobool: bool init false;
	[Init_lo] s = 0 -> (s' = 7) & (Avoidbool' = false) & (Avoidbool' = false) & (Videobool' = false) & (GPSbool' = false) & (Recbool' = false) & (Logbool' = false) & (Sharbool' = false);
	[Avoid0_run] s = 7 -> (s' = 8);
	[Avoid_ok] s = 8 -> (s' = 9) & (Avoidbool' = true);
	[Avoid_fail] s = 8 -> (s' = 9);
	[Video_lo] s = 9 -> 1 - 0.01 : (s' = 4) & (Videobool' = true) + 0.01: (s' =4);
	[Nav_lo] s = 4 -> 1 - 0.001 : (s' = 3) +0.001: (s' =10);
	[GPS_lo] s = 3 -> 1 - 0.01 : (s' = 6) & (GPSbool' = true) + 0.01: (s' =6);
	[Stab_lo] s = 6 -> 1 - 0.001 : (s' = 5) +0.001: (s' =11);
	[Rec_lo] s = 5 -> 1 - 0.01 : (s' = 1) & (Recbool' = true) + 0.01: (s' =1);
	[Log_lo] s = 1 -> 1 - 0.01 : (s' = 2) & (Logbool' = true) + 0.01: (s' =2);
	[Shar_lo] s = 2 -> 1 - 0.01 : (s' = 12) & (Sharbool' = true) + 0.01: (s' =12);
	[FinalLO0] s = 12 & Video = false & Shar = false & Rec = false -> (s' = 0);
	[FinalLO1] s = 12 & Video = true & Shar = true & Rec = true -> (s' = 0);
	[FinalLO2] s = 12 & Video = true & Shar = true & Rec = false -> (s' = 0);
	[FinalLO3] s = 12 & Video = true & Shar = false & Rec = false -> (s' = 0);
	[FinalLO4] s = 12 & Video = true & Rec = true & Shar = false -> (s' = 0);
	[FinalLO5] s = 12 & Shar = true & Video = false & Rec = false -> (s' = 0);
	[FinalLO6] s = 12 & Shar = true & Rec = true & Video = false -> (s' = 0);
	[FinalLO7] s = 12 & Rec = true & Video = false & Shar = false -> (s' = 0);

	[Nav_hi] s = 10 -> (s' =11);
	[Stab_hi] s = 11 -> (s' =0);
endmodule

rewards "Shar_cycles"
	[FinalLO1] true : 1;
	[FinalLO2] true : 1;
	[FinalLO5] true : 1;
	[FinalLO6] true : 1;
endrewards

rewards "Rec_cycles"
	[FinalLO1] true : 1;
	[FinalLO4] true : 1;
	[FinalLO6] true : 1;
	[FinalLO7] true : 1;
endrewards

rewards "Video_cycles"
	[FinalLO1] true : 1;
	[FinalLO2] true : 1;
	[FinalLO3] true : 1;
	[FinalLO4] true : 1;
endrewards

rewards "total_cycles"
	[FinalLO0] true : 1;
	[FinalLO1] true : 1;
	[FinalLO2] true : 1;
	[FinalLO3] true : 1;
	[FinalLO4] true : 1;
	[FinalLO5] true : 1;
	[FinalLO6] true : 1;
	[FinalLO7] true : 1;
	[Stab_hi] true : 1;
endrewards

