export default function Slide9() {
  return (
    <div className="w-screen h-screen overflow-hidden relative deck-frame">
      <div className="absolute right-0 top-0 h-[100vh] w-[34vw] bg-[#286747]" />
      <div className="absolute right-[7vw] top-[19vh] h-[47vh] w-[20vw] rounded-[10vw_10vw_2vw_2vw] bg-[#b9df74]" />
      <div className="absolute right-[13vw] top-[29vh] h-[27vh] w-[8vw] rounded-[6vw_6vw_1vw_1vw] bg-[#f2f6ec]" />
      <div className="relative z-10 px-[7vw] pt-[8vh]">
        <h1 className="deck-title max-w-[61vw]">Reflection: plan, test, explain</h1>
        <ul className="deck-copy mt-[5vh] flex max-w-[62vw] flex-col gap-[3vh]">
          <li>Separating games, users, genres, and saved-game details keeps the data organized</li>
          <li>AI-assisted project tracking helped list finished work, next steps, and checks</li>
          <li>I am responsible for understanding the code and explaining it in my own words</li>
          <li>Before Friday: rehearse the demo and confirm the app opens in VNC</li>
          <li>Future improvement: edit or remove a saved-game entry and test Swing interactions</li>
        </ul>
      </div>
    </div>
  );
}
