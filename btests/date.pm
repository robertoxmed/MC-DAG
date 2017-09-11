dtmc

const int D;

formula Video = Videobool;
formula Rec = Recbool & GPSbool;
formula Shar = Sharbool & Logbool;

module proc
	s : [0..14] init 0;
	Sharbool: bool init false;
	Videobool: bool init false;
	Logbool: bool init false;
	GPSbool: bool init false;
	Recbool: bool init false;
	[Init_lo] s = 0 -> (s' = 1) & (Videobool' = false) & (GPSbool' = false) & (Recbool' = false) & (Logbool' = false) & (Sharbool' = false);
	[Avoid_lo] s = 1 -> 1 - 0.01 : (s' = 3) + 0.01: (s' =10);
	[Video_lo] s = 3 -> 1 - 0.001 : (s' = 5) & (Videobool' = true) + 0.001: (s' = 10);
	[Nav_lo] s = 5 -> 1 - 1.0E-4 : (s' = 8) +1.0E-4: (s' = 12);
	[GPS_lo] s = 8 -> 1 - 0.001 : (s' = 4) & (GPSbool' = true) + 0.001: (s' =12);
	[Stab_lo] s = 4 -> 1 - 1.0E-5 : (s' = 9) +1.0E-5: (s' =11);
	[Rec_lo] s = 9 -> 1 - 0.01 : (s' = 6) & (Recbool' = true) + 0.01: (s' =11);
	[Log_lo] s = 6 -> 1 - 0.01: (s' = 2) & (Logbool' = true) + 0.01: (s'= 11);
	[Shar_lo] s = 2 -> 1 - 0.01 : (s' = 13) & (Sharbool' = true) + 0.01: (s' =13);
	[FinalLO0] s = 13 & Video = false & Rec = false & Shar = false -> (s' = 0);
	[FinalLO1] s = 13 & Video = true & Rec = false & Shar = false -> (s' = 0);
	[FinalLO2] s = 13 & Rec = true & Video = false & Shar = false -> (s' = 0);
	[FinalLO3] s = 13 & Video = true & Rec = true & Shar = false -> (s' = 0);
	[FinalLO4] s = 13 & Video = true & Shar = true & Rec = false -> (s' = 0);
	[FinalLO5] s = 13 & Rec = true & Shar = true & Video = false -> (s' = 0);
	[FinalLO6] s = 13 & Video = true & Rec = true & Shar = true -> (s' = 0);
	[FinalLO7] s = 13 & Shar = true & Video = false & Rec = false -> (s' = 0);

	[Avoid_hi] s = 10 -> (s' =12);
	[Nav_hi] s = 12 -> (s' =11);
	[Stab_hi] s = 11 -> (s' = 11);
	[FinalLO0] s = 11 & Video = false & Rec = false & Shar = false -> (s' = 0);
	[FinalLO1] s = 11 & Video = true & Rec = false & Shar = false -> (s' = 0);
	[FinalLO2] s = 11 & Rec = true & Video = false & Shar = false -> (s' = 0);
	[FinalLO3] s = 11 & Video = true & Rec = true & Shar = false -> (s' = 0);
	[FinalLO4] s = 11 & Video = true & Shar = true & Rec = false -> (s' = 0);
	[FinalLO5] s = 11 & Rec = true & Shar = true & Video = false -> (s' = 0);
	[FinalLO6] s = 11 & Video = true & Rec = true & Shar = true -> (s' = 0);
	[FinalLO7] s = 11 & Shar = true & Video = false & Rec = false -> (s' = 0);

endmodule

rewards "Shar_cycles"
	[FinalLO4] true : 1;
	[FinalLO5] true : 1;
	[FinalLO6] true : 1;
	[FinalLO7] true : 1;
endrewards

rewards "Video_cycles"
	[FinalLO1] true : 1;
	[FinalLO3] true : 1;
	[FinalLO4] true : 1;
	[FinalLO6] true : 1;
endrewards

rewards "Rec_cycles"
	[FinalLO2] true : 1;
	[FinalLO3] true : 1;
	[FinalLO5] true : 1;
	[FinalLO6] true : 1;
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
endrewards

