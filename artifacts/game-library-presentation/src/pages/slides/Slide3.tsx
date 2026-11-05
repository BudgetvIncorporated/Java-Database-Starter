export default function Slide3() {
  return (
    <div className="w-screen h-screen overflow-hidden relative deck-frame">
      <div className="absolute right-0 top-0 h-[29vh] w-[42vw] rounded-bl-[12vw] bg-[#b9df74]" />
      <div className="relative px-[7vw] pt-[8vh]">
        <h1 className="deck-title max-w-[80vw]" data-layout-content="title">Project components and tools</h1>
        <div className="mt-[5vh] grid grid-cols-[.32fr_1fr] gap-x-[3vw]">
          <div className="space-y-[4.4vh] font-display text-[2.3vw] font-bold leading-[1.08] text-[#286747]" data-layout-content="component names">
            <p>Java Swing</p>
            <p>SQLite + sqlite-jdbc</p>
            <p>DAO classes</p>
            <p>Maven + JUnit 4</p>
            <p>GitHub</p>
            <p>Replit VNC</p>
          </div>
          <div className="space-y-[4.4vh] border-l-[.18vw] border-[#b9df74] pl-[3vw] text-[2vw] leading-[1.08]" data-layout-content="component details">
            <p>— desktop screens and controls</p>
            <p>— local relational database connection</p>
            <p>— SQL stays separate from the user interface</p>
            <p>— build and automated tests</p>
            <p>— source-code repository and history</p>
            <p>— runs the desktop window in the browser</p>
          </div>
        </div>
      </div>
    </div>
  );
}
