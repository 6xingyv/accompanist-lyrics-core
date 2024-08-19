package io.mocha.music.lyrics

import io.mocha.music.lyrics.parser.TTMLParser
import kotlin.test.Test

class LyricsParserTest {

    @Test
    internal fun ttmlParseTest() {
        val ttml ="""
            <?xml version="1.0" encoding="UTF-8"?>
            <tt xmlns="http://www.w3.org/ns/ttml"
                xmlns:tts="http://www.w3.org/ns/ttml#styling"
                xmlns:itunes="http://itunes.apple.com/lyric-ttml-extensions"
                xmlns:ttm="http://www.w3.org/ns/ttml#metadata"
                xml:lang="en-US">
              <head>
                <metadata>
                  <ttm:title>Dancing With A Stranger</ttm:title>
                  <ttm:agent type="person" xml:id="v1">
                     <ttm:name type="full">Sam Smith</ttm:name>
                  </ttm:agent>
                  <ttm:agent type="person" xml:id="v2">
                     <ttm:name type="full">Normani</ttm:name>
                  </ttm:agent>
                  <ttm:agent type="group" xml:id="v3"/>
                </metadata>
              </head>
              <body dur="02:51.030">
                <div begin="00:07.621" end="00:45.268" itunes:song-part="Verse">
                  <p begin="00:07.621" end="00:10.267" ttm:agent="v1">
                    <span begin="00:07.621" end="00:07.920">I</span> <span begin="00:07.920" end="00:08.253">don't</span> 
                    <span begin="00:08.253" end="00:08.739">wanna</span> <span begin="00:08.739" end="00:09.103">be</span> 
                    <span begin="00:09.103" end="00:09.655">alone</span> <span begin="00:09.655" end="00:10.267">tonight</span>
                  </p>
                  <p begin="00:10.961" end="00:15.046" ttm:agent="v1">
                    <span begin="00:10.961" end="00:12.244">It's</span> <span begin="00:12.244" end="00:12.828">pretty</span> 
                    <span begin="00:12.828" end="00:13.177">clear</span> <span begin="00:13.177" end="00:13.462">that</span> 
                    <span begin="00:13.462" end="00:13.761">I'm</span> <span begin="00:13.761" end="00:14.046">not</span> 
                    <span begin="00:14.046" end="00:14.494">over</span> <span begin="00:14.494" end="00:15.046">you</span>
                  </p>
                  <p begin="00:16.828" end="00:20.471" ttm:agent="v1">
                    <span begin="00:16.828" end="00:17.210">I'm</span> <span begin="00:17.210" end="00:17.527">still</span> 
                    <span begin="00:17.527" end="00:18.127">thinking</span> <span begin="00:18.127" end="00:18.394">'bout</span> 
                    <span begin="00:18.394" end="00:18.676">the</span> <span begin="00:18.676" end="00:19.178">things</span> 
                    <span begin="00:19.178" end="00:19.594">you</span> <span begin="00:19.594" end="00:20.471">do</span>
                  </p>
                  <p begin="00:21.271" end="00:24.175" ttm:agent="v1">
                    <span begin="00:21.271" end="00:21.588">So</span> <span begin="00:21.588" end="00:21.859">I</span> 
                    <span begin="00:21.859" end="00:22.188">don't</span> <span begin="00:22.188" end="00:22.722">wanna</span> 
                    <span begin="00:22.722" end="00:23.055">be</span> <span begin="00:23.055" end="00:23.620">alone</span> 
                    <span begin="00:23.620" end="00:24.175">tonight</span>
                  </p>
                  <p begin="00:24.289" end="00:26.656" ttm:agent="v1">
                    <span begin="00:24.289" end="00:24.705">Alone</span> <span begin="00:24.705" end="00:25.321">tonight,</span> 
                    <span begin="00:25.471" end="00:25.905">alone</span> <span begin="00:25.905" end="00:26.656">tonight</span>
                  </p>
                  <p begin="00:27.320" end="00:29.237" ttm:agent="v1">
                    <span begin="00:27.320" end="00:27.686">Can</span> <span begin="00:27.686" end="00:28.003">you</span> 
                    <span begin="00:28.003" end="00:28.304">light</span> <span begin="00:28.304" end="00:28.486">the</span> 
                    <span begin="00:28.486" end="00:29.237">fire?</span>
                  </p>
                  <p begin="00:30.545" end="00:34.297" ttm:agent="v1">
                    <span begin="00:30.545" end="00:30.894">I</span> <span begin="00:30.894" end="00:31.211">need</span> 
                    <span begin="00:31.211" end="00:32.078">somebody</span> <span begin="00:32.078" end="00:32.361">who</span> 
                    <span begin="00:32.361" end="00:32.662">can</span> <span begin="00:32.662" end="00:32.995">take</span> 
                    <span begin="00:32.995" end="00:34.297">control</span>
                  </p>
                  <p begin="00:35.177" end="00:39.449" ttm:agent="v1">
                    <span begin="00:35.177" end="00:35.529">I</span> <span begin="00:35.529" end="00:35.878">know</span> 
                    <span begin="00:35.878" end="00:36.761">exactly</span> <span begin="00:36.761" end="00:37.028">what</span> 
                    <span begin="00:37.028" end="00:37.326">I</span> <span begin="00:37.326" end="00:37.678">need</span> 
                    <span begin="00:37.678" end="00:38.177">to</span> <span begin="00:38.177" end="00:39.449">do</span>
                  </p>
                  <p begin="00:39.868" end="00:42.815" ttm:agent="v1">
                    <span begin="00:39.868" end="00:40.217">'Cause</span> <span begin="00:40.217" end="00:40.516">I</span> 
                    <span begin="00:40.516" end="00:40.833">don't</span> <span begin="00:40.833" end="00:41.401">wanna</span> 
                    <span begin="00:41.401" end="00:41.684">be</span> <span begin="00:41.684" end="00:42.268">alone</span> 
                    <span begin="00:42.268" end="00:42.815">tonight</span>
                  </p>
                  <p begin="00:42.935" end="00:45.268" ttm:agent="v1">
                    <span begin="00:42.935" end="00:43.401">Alone</span> <span begin="00:43.401" end="00:43.961">tonight,</span> 
                    <span begin="00:44.142" end="00:44.625">alone</span> <span begin="00:44.625" end="00:45.268">tonight</span>
                  </p>
                </div>
                <div begin="00:46.537" end="01:10.357" itunes:song-part="Chorus">
                  <p begin="00:46.537" end="00:49.038" ttm:agent="v1">
                    <span begin="00:46.537" end="00:47.238">Look</span> <span begin="00:47.238" end="00:47.537">what</span> 
                    <span begin="00:47.537" end="00:47.820">you</span> <span begin="00:47.820" end="00:48.121">made</span> 
                    <span begin="00:48.121" end="00:48.406">me</span> <span begin="00:48.406" end="00:49.038">do</span>
                  </p>
                  <p begin="00:49.214" end="00:51.121" ttm:agent="v1">
                    <span begin="00:49.214" end="00:49.529">I'm</span> <span begin="00:49.529" end="00:49.846">with</span> 
                    <span begin="00:49.846" end="00:50.745">somebody</span> <span begin="00:50.745" end="00:51.121">new</span>
                  </p>
                  <p begin="00:51.228" end="00:55.675" ttm:agent="v1">
                    <span begin="00:51.228" end="00:51.879">Ooh,</span> <span begin="00:51.879" end="00:52.463">baby,</span> 
                    <span begin="00:52.463" end="00:53.012">baby,</span> <span begin="00:53.012" end="00:53.345">I'm</span> 
                    <span begin="00:53.345" end="00:54.180">dancing</span> <span begin="00:54.180" end="00:54.513">with</span> 
                    <span begin="00:54.513" end="00:54.831">a</span> <span begin="00:54.831" end="00:55.675">stranger</span>
                  </p>
                  <p begin="00:55.921" end="00:58.332" ttm:agent="v1">
                    <span begin="00:55.921" end="00:56.521">Look</span> <span begin="00:56.521" end="00:56.820">what</span> 
                    <span begin="00:56.820" end="00:57.153">you</span> <span begin="00:57.153" end="00:57.487">made</span> 
                    <span begin="00:57.487" end="00:57.769">me</span> <span begin="00:57.769" end="00:58.332">do</span>
                  </p>
                  <p begin="00:58.524" end="01:00.468" ttm:agent="v1">
                    <span begin="00:58.524" end="00:58.876">I'm</span> <span begin="00:58.876" end="00:59.209">with</span> 
                    <span begin="00:59.209" end="01:00.041">somebody</span> <span begin="01:00.041" end="01:00.468">new</span>
                  </p>
                  <p begin="01:00.572" end="01:05.714" ttm:agent="v1">
                    <span begin="01:00.572" end="01:01.153">Ooh,</span> <span begin="01:01.153" end="01:01.788">baby,</span> 
                    <span begin="01:01.788" end="01:02.321">baby,</span> <span begin="01:02.321" end="01:02.639">I'm</span> 
                    <span begin="01:02.639" end="01:03.505">dancing</span> <span begin="01:03.505" end="01:03.823">with</span> 
                    <span begin="01:03.823" end="01:04.137">a</span> <span begin="01:04.137" end="01:04.763">strang</span>
                    <span begin="01:04.763" end="01:05.714">er</span>
                  </p>
                  <p begin="01:07.255" end="01:10.357" ttm:agent="v1">
                    <span begin="01:07.255" end="01:08.186">Dancing</span> <span begin="01:08.186" end="01:08.471">with</span> 
                    <span begin="01:08.471" end="01:08.805">a</span> <span begin="01:08.805" end="01:09.410">strang</span>
                    <span begin="01:09.410" end="01:10.357">er</span>
                  </p>
                </div>
                <div begin="01:12.395" end="01:31.914" itunes:song-part="Verse">
                  <p begin="01:12.395" end="01:15.728" ttm:agent="v2">
                    <span begin="01:12.395" end="01:12.795">I</span> <span begin="01:12.795" end="01:13.328">wasn't</span> 
                    <span begin="01:13.328" end="01:13.960">even</span> <span begin="01:13.960" end="01:14.576">going</span> 
                    <span begin="01:14.576" end="01:14.867">out</span> <span begin="01:14.867" end="01:15.728">tonight</span>
                  </p>
                  <p begin="01:17.141" end="01:20.976" ttm:agent="v2">
                    <span begin="01:17.141" end="01:17.491">But</span> <span begin="01:17.491" end="01:17.808">boy,</span> 
                    <span begin="01:17.808" end="01:18.141">I</span> <span begin="01:18.141" end="01:18.408">need</span> 
                    <span begin="01:18.408" end="01:18.691">to</span> <span begin="01:18.691" end="01:18.976">get</span> 
                    <span begin="01:18.976" end="01:19.259">you</span> <span begin="01:19.259" end="01:19.607">off</span> 
                    <span begin="01:19.607" end="01:19.887">of</span> <span begin="01:19.887" end="01:20.257">my</span> 
                    <span begin="01:20.257" end="01:20.976">mind</span>
                  </p>
                  <p begin="01:21.843" end="01:25.984" ttm:agent="v2">
                    <span begin="01:21.843" end="01:22.158">I</span> <span begin="01:22.158" end="01:22.443">know</span> 
                    <span begin="01:22.443" end="01:23.360">exactly</span> <span begin="01:23.360" end="01:23.643">what</span> 
                    <span begin="01:23.643" end="01:23.926">I</span> <span begin="01:23.926" end="01:24.344">have</span> 
                    <span begin="01:24.344" end="01:24.774">to</span> <span begin="01:24.774" end="01:25.984">do</span>
                  </p>
                  <p begin="01:26.678" end="01:29.484" ttm:agent="v2">
                    <span begin="01:26.678" end="01:27.062">I</span> <span begin="01:27.062" end="01:27.395">don't</span> 
                    <span begin="01:27.395" end="01:27.929">wanna</span> <span begin="01:27.929" end="01:28.262">be</span> 
                    <span begin="01:28.262" end="01:28.862">alone</span> <span begin="01:28.862" end="01:29.484">tonight</span>
                  </p>
                  <p begin="01:29.628" end="01:31.914" ttm:agent="v2">
                    <span begin="01:29.628" end="01:30.060">Alone</span> <span begin="01:30.060" end="01:30.575">tonight,</span> 
                    <span begin="01:30.746" end="01:31.212">alone</span> <span begin="01:31.212" end="01:31.914">tonight</span>
                  </p>
                </div>
                <div begin="01:33.183" end="02:06.889" itunes:song-part="Chorus">
                  <p begin="01:33.183" end="01:35.516" ttm:agent="v2">
                    <span begin="01:33.183" end="01:33.799">Look</span> <span begin="01:33.799" end="01:34.116">what</span> 
                    <span begin="01:34.116" end="01:34.434">you</span> <span begin="01:34.434" end="01:34.767">made</span> 
                    <span begin="01:34.767" end="01:35.050">me</span> <span begin="01:35.050" end="01:35.516">do</span>
                  </p>
                  <p begin="01:35.841" end="01:37.660" ttm:agent="v2">
                    <span begin="01:35.841" end="01:36.174">I'm</span> <span begin="01:36.174" end="01:36.457">with</span> 
                    <span begin="01:36.457" end="01:37.324">somebody</span> <span begin="01:37.324" end="01:37.660">new</span>
                  </p>
                  <p begin="01:37.921" end="01:42.396" ttm:agent="v2">
                    <span begin="01:37.921" end="01:38.454">Oh,</span> <span begin="01:38.454" end="01:39.054">baby,</span> 
                    <span begin="01:39.054" end="01:39.604">baby,</span> <span begin="01:39.604" end="01:39.905">I'm</span> 
                    <span begin="01:39.905" end="01:40.804">dancing</span> <span begin="01:40.804" end="01:41.121">with</span> 
                    <span begin="01:41.121" end="01:41.406">a</span> <span begin="01:41.406" end="01:42.396">stranger</span>
                  </p>
                  <p begin="01:42.543" end="01:44.887" ttm:agent="v2">
                    <span begin="01:42.543" end="01:43.111">Look</span> <span begin="01:43.111" end="01:43.410">what</span> 
                    <span begin="01:43.410" end="01:43.746">you</span> <span begin="01:43.746" end="01:44.044">made</span> 
                    <span begin="01:44.044" end="01:44.343">me</span> <span begin="01:44.343" end="01:44.887">do</span>
                  </p>
                  <p begin="01:45.154" end="01:46.957" ttm:agent="v2">
                    <span begin="01:45.154" end="01:45.453">I'm</span> <span begin="01:45.453" end="01:45.754">with</span> 
                    <span begin="01:45.754" end="01:46.671">somebody</span> <span begin="01:46.671" end="01:46.957">new</span>
                  </p>
                  <p begin="01:47.178" end="01:51.922" ttm:agent="v2">
                    <span begin="01:47.178" end="01:47.778">Oh,</span> <span begin="01:47.778" end="01:48.362">baby,</span> 
                    <span begin="01:48.362" end="01:48.911">baby,</span> <span begin="01:48.911" end="01:49.245">I'm</span> 
                    <span begin="01:49.245" end="01:50.111">dancing</span> <span begin="01:50.111" end="01:50.445">with</span> 
                    <span begin="01:50.445" end="01:50.746">a</span> <span begin="01:50.746" end="01:51.922">stranger</span>
                  </p>
                  <p begin="01:53.860" end="01:57.321" ttm:agent="v3">
                    <span begin="01:53.860" end="01:54.727">Dancing</span> <span begin="01:54.727" end="01:55.092">with</span> 
                    <span begin="01:55.092" end="01:55.393">a</span> <span begin="01:55.393" end="01:56.106">strang</span>
                    <span begin="01:56.106" end="01:57.321">er</span>
                  </p>
                  <p begin="01:58.560" end="02:01.904" ttm:agent="v3">
                    <span begin="01:58.560" end="01:59.424">Dancing</span> <span begin="01:59.424" end="01:59.757">with</span> 
                    <span begin="01:59.757" end="02:00.074">a</span> <span begin="02:00.074" end="02:00.823">strang</span>
                    <span begin="02:00.823" end="02:01.904">er</span>
                  </p>
                  <p begin="02:03.210" end="02:05.197" ttm:agent="v1">
                    <span begin="02:03.210" end="02:04.461">Dancing,</span> <span begin="02:04.461" end="02:05.197">yeah</span>
                  </p>
                  <p begin="02:04.801" end="02:06.889" ttm:agent="v2">
                    <span begin="02:04.801" end="02:05.741">Ooh,</span> <span begin="02:05.741" end="02:06.440">ooh-</span>
                    <span begin="02:06.440" end="02:06.889">yeah</span>
                  </p>
                </div>
                <div begin="02:10.453" end="02:29.249" itunes:song-part="Outro">
                  <p begin="02:10.453" end="02:12.827" ttm:agent="v1">
                    <span begin="02:10.453" end="02:11.103">Look</span> <span begin="02:11.103" end="02:11.437">what</span> 
                    <span begin="02:11.437" end="02:11.738">you</span> <span begin="02:11.738" end="02:12.071">made</span> 
                    <span begin="02:12.071" end="02:12.370">me</span> <span begin="02:12.370" end="02:12.827">do</span>
                  </p>
                  <p begin="02:13.187" end="02:15.036" ttm:agent="v1">
                    <span begin="02:13.187" end="02:13.520">I'm</span> <span begin="02:13.520" end="02:13.822">with</span> 
                    <span begin="02:13.822" end="02:14.720">somebody</span> <span begin="02:14.720" end="02:15.036">new</span>
                  </p>
                  <p begin="02:15.179" end="02:19.646" ttm:agent="v1">
                    <span begin="02:15.179" end="02:15.830">Ooh,</span> <span begin="02:15.830" end="02:16.395">baby,</span> 
                    <span begin="02:16.395" end="02:16.896">baby,</span> <span begin="02:16.896" end="02:17.246">I'm</span> 
                    <span begin="02:17.246" end="02:18.080">dancing</span> <span begin="02:18.080" end="02:18.395">with</span> 
                    <span begin="02:18.395" end="02:18.712">a</span> <span begin="02:18.712" end="02:19.646">stranger</span>
                  </p>
                  <p begin="02:19.835" end="02:22.136" ttm:agent="v3">
                    <span begin="02:19.835" end="02:20.417">Look</span> <span begin="02:20.417" end="02:20.753">what</span> 
                    <span begin="02:20.753" end="02:21.051">you</span> <span begin="02:21.051" end="02:21.369">made</span> 
                    <span begin="02:21.369" end="02:21.683">me</span> <span begin="02:21.683" end="02:22.136">do</span>
                  </p>
                  <p begin="02:22.497" end="02:24.374" ttm:agent="v3">
                    <span begin="02:22.497" end="02:22.798">I'm</span> <span begin="02:22.798" end="02:23.065">with</span> 
                    <span begin="02:23.065" end="02:23.982">somebody</span> <span begin="02:23.982" end="02:24.374">new</span>
                  </p>
                  <p begin="02:24.510" end="02:29.249" ttm:agent="v3">
                    <span begin="02:24.510" end="02:25.059">Ooh,</span> <span begin="02:25.059" end="02:25.710">baby,</span> 
                    <span begin="02:25.710" end="02:26.209">baby,</span> <span begin="02:26.209" end="02:26.542">I'm</span> 
                    <span begin="02:26.542" end="02:27.411">dancing</span> <span begin="02:27.411" end="02:27.745">with</span> 
                    <span begin="02:27.745" end="02:28.043">a</span> <span begin="02:28.043" end="02:29.249">stranger</span>
                  </p>
                </div>
                <div begin="02:29.648" end="02:48.133" itunes:song-part="Outro">
                  <p begin="02:29.648" end="02:32.602" ttm:agent="v3">
                    <span begin="02:29.648" end="02:30.000">I'm</span> <span begin="02:30.000" end="02:30.917">dancing,</span> 
                    <span begin="02:30.917" end="02:31.232">I'm</span> <span begin="02:31.232" end="02:32.602">dancing</span>
                  </p>
                  <p begin="02:34.324" end="02:39.579" ttm:agent="v1">
                    <span ttm:role="x-bg">
                    <span begin="02:34.324" end="02:34.690">(I'm</span> <span begin="02:34.690" end="02:35.572">dancing,</span> 
                    <span begin="02:35.572" end="02:35.874">I'm</span> <span begin="02:35.874" end="02:37.266">dancing)</span>
                    </span> 
                    <span begin="02:35.747" end="02:36.677">Dancing</span> <span begin="02:36.677" end="02:37.027">with</span> 
                    <span begin="02:37.027" end="02:37.344">a</span> <span begin="02:37.344" end="02:38.253">strang</span>
                    <span begin="02:38.253" end="02:39.579">er</span>
                  </p>
                  <p begin="02:38.994" end="02:43.927" ttm:agent="v1">
                    <span ttm:role="x-bg">
                    <span begin="02:38.994" end="02:39.413">(I'm</span> <span begin="02:39.413" end="02:40.178">dancing,</span> 
                    <span begin="02:40.178" end="02:40.562">I'm</span> <span begin="02:40.562" end="02:41.978">dancing)</span>
                    </span> 
                    <span begin="02:40.407" end="02:41.325">Dancing</span> <span begin="02:41.325" end="02:41.671">with</span> 
                    <span begin="02:41.671" end="02:41.941">a</span> <span begin="02:41.941" end="02:42.860">strang</span>
                    <span begin="02:42.860" end="02:43.927">er</span>
                  </p>
                  <p begin="02:43.690" end="02:48.133" ttm:agent="v2">
                    <span ttm:role="x-bg">
                    <span begin="02:43.690" end="02:44.039">(I'm</span> <span begin="02:44.039" end="02:44.874">dancing,</span> 
                    <span begin="02:44.874" end="02:45.191">I'm</span> <span begin="02:45.191" end="02:46.495">dancing)</span>
                    </span> 
                    <span begin="02:45.163" end="02:46.061">Dancing</span> <span begin="02:46.061" end="02:46.411">with</span> 
                    <span begin="02:46.411" end="02:46.747">a</span> <span begin="02:46.747" end="02:47.558">strang</span>
                    <span begin="02:47.558" end="02:48.133">er</span>
                  </p>
                </div>
              </body>
            </tt>
        """.trimIndent()
        val data = TTMLParser.parse(ttml)
        print(data.toString())
    }
}