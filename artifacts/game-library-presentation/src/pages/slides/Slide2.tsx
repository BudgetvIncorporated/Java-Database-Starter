export default function Slide2() {
  return (
    <div className="w-screen h-screen overflow-hidden relative green-surface">
      <div className="absolute right-0 top-0 h-[100vh] w-[28vw] bg-[#b9df74]" data-layout-reserved="right accent panel" />
      <div className="relative z-10 px-[7vw] pt-[8vh]">
        <h1 className="deck-title max-w-[66vw] text-[#f2f6ec]" data-layout-content="title">What works now</h1>
        <ul className="deck-copy mt-[5vh] flex max-w-[79vw] flex-col gap-[2.5vh] text-[#f2f6ec]" data-layout-content="feature list">
          <li>Browse, search, and filter games by genre</li>
          <li>Add, edit, and delete games and users</li>
          <li>Save games to user libraries with ratings and play counts</li>
          <li>Open saved game links or copy them if no browser is available</li>
          <li>JOIN and aggregate reports; Java insertion sort</li>
          <li>14 automated tests pass; Swing window launches in Replit VNC</li>
        </ul>
      </div>
    </div>
  );
}
