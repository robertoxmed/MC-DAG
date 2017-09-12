dtmc

const int D;

module Logfirm
	v: [0..4] init 3;
	[Log_end_ok] v = 0 -> (v' = 1);
	[Log_end_fail] v = 0 -> (v' = 0);
	[Log_end_ok] v = 1 -> (v' = 3);
	[Log_end_fail] v = 1 -> (v' = 2);
	[Log_end_ok] v = 2 -> (v' = 1);
	[Log_end_fail] v = 2 -> (v' = 0);
	[Log_end_ok] v = 3 -> (v' = 3);
	[Log_end_fail] v = 3 -> (v' = 2);

	[Log_fail] v = 0 -> (v' = 0);
	[Log_ok] v = 1 -> (v' = 1);
	[Log_ok] v = 2 -> (v' = 2);
	[Log_ok] v = 3 -> (v' = 3);
endmodule

module Log
	t: [0..2] init 0;
	[Log0_run] t = 0 ->  1 - 0.01 : (t' = 1) + 0.01 : (t' = 2);
	[Log_end_ok] t = 1 -> (t' = 0);
	[Log_end_fail] t = 2 -> (t' = 0);
endmodule

formula Video = Videobool;
formula Rec = Recbool & GPSbool;
formula Shar = Sharbool & Logbool;

module proc
	s : [0..20] init 0;
	Sharbool: bool init false;
	Videobool: bool init false;
	Logbool: bool init false;
	GPSbool: bool init false;
	Recbool: bool init false;
	[Init_lo] s = 0 -> (s' = 1) & (Videobool' = false) & (GPSbool' = false) & (Recbool' = false) & (Logbool' = false) & (Sharbool' = false);
	[Avoid_lo] s = 1 -> 1 - 0.01 : (s' = 3) +0.01: (s' =10);
	[Video_run] s = 3 -> 1 - 0.001 : (s' = 19) & (Videobool' = true) + 0.001: (s' =5);
	[Video_ok] s = 19 -> (s' = 5);
	[Nav_lo] s = 5 -> 1 - 1.0E-4 : (s' = 8) +1.0E-4: (s' =12);
	[GPS_lo] s = 8 -> 1 - 0.001 : (s' = 4) & (GPSbool' = true) + 0.001: (s' =4);
	[Stab_lo] s = 4 -> 1 - 1.0E-5 : (s' = 9) +1.0E-5: (s' =11);
	[Rec_run] s = 9 -> 1 - 0.01 : (s' = 18) & (Recbool' = true) + 0.01: (s' =6);
	[Rec_ok] s = 18 & Rec  -> (s' = 6);
	[Rec_fail] s = 18 & Rec = false -> (s' = 6);
	[Log0_run] s = 6 -> (s' = 7);
	[Log_ok] s = 7 -> (s' = 2) & (Logbool' = true);
	[Log_fail] s = 7 -> (s' = 2);
	[Shar_run] s = 2 -> 1 - 0.01 : (s' = 17) & (Sharbool' = true) + 0.01: (s' =13);
	[Shar_ok] s = 17 & Shar -> (s' = 13);
	[Shar_fail] s = 17 & Shar = false -> (s' = 13);
	[FinalLO] s = 13  -> (s' = 0);

	[Avoid_hi] s = 10 -> (s' =12);
	[Nav_hi] s = 12 -> (s' =11);
	[Stab_hi] s = 11 -> (s' =0);
endmodule

rewards "Shar_cycles"
	[Shar_ok] true : 1;
endrewards

rewards "Video_cycles"
	[Video_ok] true : 1;
endrewards

rewards "Rec_cycles"
	[Rec_ok] true : 1;
endrewards

rewards "total_cycles"
	[FinalLO] true : 1;
	[Stab_hi] true : 1;
endrewards

