export default function Slide7() {
  return (
    <div className="w-screen h-screen overflow-hidden relative deck-frame">
      <div className="absolute bottom-0 left-0 h-[28vh] w-[31vw] rounded-tr-[12vw] bg-[#b9df74]" />
      <div className="relative px-[6.5vw] pt-[7vh]">
        <h1 className="deck-title max-w-[80vw]">Project timeline to Friday</h1>
        <div className="mt-[4vh] grid grid-cols-[1fr_1.02fr] gap-x-[3vw]">
          <div className="grid grid-rows-4 gap-[1.5vh]">
            <p className="deck-copy flex items-center border-b border-[#286747]/20 pb-[1.5vh]">Earlier — Diagram, schema, seed data, JDBC/DAO, queries, algorithm, and tests — COMPLETE</p>
            <p className="deck-copy flex items-center border-b border-[#286747]/20 pb-[1.5vh]">Wed, Nov 4 — Swing GUI, README, demo guide, and VNC launch — COMPLETE</p>
            <p className="deck-copy flex items-center border-b border-[#286747]/20 pb-[1.5vh]">Wed–Thu, Nov 5 — Finish slides and rehearse — IN PROGRESS / PLANNED</p>
            <p className="deck-copy flex items-center">Fri, Nov 6 — Classroom presentation — TARGET</p>
          </div>
          <div className="grid grid-rows-[8vh_repeat(4,1fr)] border-l-[.2vw] border-[#286747]/25 pl-[2.3vw]">
            <div className="timeline-grid items-end gap-[.7vw] pb-[1vh] font-display text-[1.55vw] font-bold leading-[1.05] text-[#286747]">
              <span />
              <span>Earlier</span>
              <span>Wed, Nov 4</span>
              <span>Thu, Nov 5</span>
              <span>Fri, Nov 6</span>
            </div>
            <div className="timeline-grid items-center gap-[.7vw] border-t border-[#286747]/20">
              <span className="pr-[1vw] text-[1.6vw] font-bold">Complete</span>
              <div className="timeline-track"><span className="timeline-fill" /></div>
              <span />
              <span />
              <span />
            </div>
            <div className="timeline-grid items-center gap-[.7vw] border-t border-[#286747]/20">
              <span className="pr-[1vw] text-[1.6vw] font-bold">Complete</span>
              <span />
              <div className="col-span-1 timeline-track"><span className="timeline-fill" /></div>
              <span />
              <span />
            </div>
            <div className="timeline-grid items-center gap-[.7vw] border-t border-[#286747]/20">
              <span className="pr-[1vw] text-[1.6vw] font-bold">In progress / planned</span>
              <span />
              <div className="col-span-2 timeline-track"><span className="timeline-fill lime" /></div>
              <span />
            </div>
            <div className="timeline-grid items-center gap-[.7vw] border-t border-[#286747]/20">
              <span className="pr-[1vw] text-[1.6vw] font-bold">Target</span>
              <span />
              <span />
              <span />
              <div className="timeline-track"><span className="timeline-fill gold" /></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
