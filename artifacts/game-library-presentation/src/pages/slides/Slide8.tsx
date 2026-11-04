export default function Slide8() {
  return (
    <div className="w-screen h-screen overflow-hidden relative green-surface">
      <div className="absolute right-0 top-0 h-[100vh] w-[32vw] bg-[#b9df74]" />
      <div className="relative px-[7vw] pt-[8vh]">
        <h1 className="deck-title max-w-[70vw] text-[#f2f6ec]">Five-minute app walkthrough</h1>
        <ul className="deck-copy mt-[5vh] flex max-w-[80vw] flex-col gap-[2.65vh] text-[#f2f6ec]">
          <li>Search for a game and filter by genre</li>
          <li>Sort titles with the Java insertion-sort button</li>
          <li>Show the three-table JOIN and COUNT/SUM/AVG report</li>
          <li>Add, edit, and delete a temporary game or user</li>
          <li>Save a game to a user's library and explain user_games</li>
          <li>Open the Game Library Swing workflow before presenting</li>
        </ul>
      </div>
    </div>
  );
}
