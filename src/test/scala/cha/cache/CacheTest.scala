package testCHA.CacheTest

import org.scalatest.flatspec._
import org.scalatest.matchers.should._
import chiseltest._
import chiseltest.formal._
import chiseltest.formal.chaAnno._
import chisel3._
import nutcore._
import nutcore.CacheConfig
import utils._

import chisel3.util._
import chisel3.util.experimental.BoringUtils
import bus.axi4._
import chisel3.experimental.IO

import chisel3.experimental.{ChiselAnnotation, annotate}
import firrtl.annotations.{Annotation, MemoryArrayInitAnnotation, MemoryScalarInitAnnotation, ReferenceTarget}

import bus.simplebus._


class CacheStage1Prop(implicit val cacheConfig: CacheConfig) extends CacheModule{
  
  // ArbConnectIO is from arbitor, Stage1PropIO is from stage1
  class Stage1PropIO extends Bundle {
    val metaReadBus = CacheMetaArrayReadBus()
    val dataReadBus = CacheDataArrayReadBus()
    val out = Decoupled(new Stage1IO)
  }
  class ArbConnectIO extends Bundle {
    val in = Flipped(Decoupled(new SimpleBusReqBundle(userBits = userBits, idBits = idBits)))
    val coh = Flipped(Decoupled(new SimpleBusReqBundle(userBits = userBits, idBits = idBits)))
  }

  // ---------------- initialize port and submodule ----------------
  val cio = IO(new Stage1PropIO)
  val aio = IO(new ArbConnectIO)
  val arb = Module(new Arbiter(new SimpleBusReqBundle(userBits = userBits, idBits = idBits), hasCohInt + 1))
  val s1 = Module(new CacheStage1)

  // ---------------- set connection  ----------------
  arb.io.in(hasCohInt + 0) <> aio.in
  arb.io.in(0) <> aio.coh
  arb.io.out <> s1.io.in
  s1.io.metaReadBus <> cio.metaReadBus
  s1.io.dataReadBus <> cio.dataReadBus
  s1.io.out <> cio.out

  // ---------------- generate aux signals and write assumptions/assertions ----------------
  val coreReqValid = aio.in.valid
  val coreReqFire = aio.in.fire()
  val s1ReqValid = cio.out.valid
  // s1_assume_1: request form core will keep high until getting handshake
  // restriction for external signal
  chaAssume(this,"coreReqValid -> ((coreReqValid U coreReqFire) || G coreReqValid)")

  val arrayReadReqReady = cio.metaReadBus.req.ready && cio.dataReadBus.req.ready
  // s1_assume_2: if no request to s2, then metaArray and dataArray can eventually be accessed 
  // inducted by s2_goal_1, s3_goal_1, s3_goal_2, array_goal_1
  chaAssume(this,"(G !s1ReqValid) -> F G arrayReadReqReady")
  
  val s1ReqFire = cio.out.fire()
  // s1_assume_3: if request to s2 does not make a handshake, then metaArray and dataArray can eventually be accessed 
  // inducted by s2_goal_3, s3_goal_1, s3_goal_2, array_goal_1
  chaAssume(this,"(G !s1ReqFire) -> F G arrayReadReqReady")

  val dataArrayReadValid = cio.dataReadBus.req.valid
  // s1_goal_1: if request to s2 does not make a handshake, then there will be no read request to dataArray
  // depend on s1_assume_3 (might also former assumption)
  // chaAssert(this, "(G !s1ReqFire) -> F G !dataArrayReadValid")

  // s1_final_goal: request form core will trigger request for s2
  // deps on s1_assume_1 and s1_assume_2, can be proven 
  chaAssert(this,"coreReqValid -> F s1ReqValid")
}

class CacheStage2Prop(implicit val cacheConfig: CacheConfig) extends CacheModule{
  
  // PipeConnectIO is from pipeConnect between s1 and s2, Stage2PropIO is from stage2
  class Stage2PropIO extends Bundle {
    val metaReadResp = Flipped(Vec(Ways, new MetaBundle))
    val dataReadResp = Flipped(Vec(Ways, new DataBundle))
    val metaWriteBus = Input(CacheMetaArrayWriteBus())
    val dataWriteBus = Input(CacheDataArrayWriteBus())
    val out = Decoupled(new Stage2IO)
  }
  class PipeConnectIO extends Bundle {
    val in = Flipped(Decoupled(new Stage1IO))
  }

  // ---------------- initialize port and submodule ----------------
  val cio = IO(new Stage2PropIO)
  val pio = IO(new PipeConnectIO)
  val s2 = Module(new CacheStage2)

  // ---------------- set connection  ----------------
  PipelineConnect(pio.in, s2.io.in, s2.io.out.fire(), false.asBool)
  s2.io.metaReadResp <> cio.metaReadResp
  s2.io.dataReadResp <> cio.dataReadResp
  s2.io.dataWriteBus <> cio.dataWriteBus
  s2.io.metaWriteBus <> cio.metaWriteBus
  cio.out <> s2.io.out
  
  // ---------------- generate aux signals and write assumptions/assertions ----------------
  val s1ReqValid = pio.in.valid
  val s2ReqValid = s2.io.out.valid
  val s2ReqReady = s2.io.out.ready
  // s2_goal_1: if no request from s1, there will be no request to s3 or s3 is always unready
  // no deps, can be proven
  // chaAssert(this, "(G !s1ReqValid) -> ((F G !s2ReqValid) || G !s2ReqReady)")
  
  val s2ReqFire = s2.io.out.fire()
  // s2_goal_2: request to s3 will keep high until getting handshake
  // no deps, guaranteed by pipeConnect between s1 and s2, can be proven
  // chaAssert(this, "s2ReqValid -> ((s2ReqValid U s2ReqFire) || G s2ReqValid)")

  val s1ReqFire = pio.in.fire()
  // s2_goal_3: if request from s1 does not make a handshake, there will be no request to s3 or s3 is always unready
  // no_deps, can be proven
  // chaAssert(this, "(G !s1ReqFire) -> ((F G !s2ReqValid) || G !s2ReqReady)")

  // s2_goal_4: if request from s2 does not make a handshake, request from s1 will not make a handshake eventually
  // no_deps, can be proven
  // chaAssert(this, "(G !s2ReqFire) -> F G !s1ReqFire")

  val s2ForwardingMetas = s2.io.out.bits.metas
  val s2ForwardingWayMask = s2.io.out.bits.waymask
  val s2ForwardingTag = Mux1H(s2ForwardingWayMask, s2ForwardingMetas).tag
  val s2ValidHit = s2.io.out.valid && s2.io.out.bits.hit
  val reqTag = s2.io.out.bits.req.addr.asTypeOf(addrBundle).tag
  val consistIndex = !s2ValidHit || (s2ForwardingTag === reqTag)
  // s2_goal_5: if request to s3 is valid and the request meets a hit, then the output tag is the same as its in request 
  // no_deps, can be proven
  // chaAssert(this, "consistIndex")

  val s3IsMmio = s2.io.out.bits.mmio && s2.io.out.valid
  val reqIsMmio = AddressSpace.isMMIO(s2.io.out.bits.req.addr) && s2.io.out.valid
  // s2_goal_6: s2 will set the mmio-bit true <=> the request is a mmio request 
  // no_deps, can be proven
  // chaAssert(this, "(!s3IsMmio || reqIsMmio) && (!reqIsMmio || s3IsMmio)")

  val s2ForwardingNotMMIO = !AddressSpace.isMMIO(s2.io.out.bits.req.addr) || !s2ValidHit
  // not restrict index, but it havs no effect
  val metaReadIndex = getMetaIdx(pio.in.bits.req.addr)
  val metaReadTag_0 = cio.metaReadResp(0).tag
  val metaReadAddr_0 = Cat(metaReadTag_0, metaReadIndex, 0.U(OffsetBits.W))
  val metaReadNotMMIO_0 = !AddressSpace.isMMIO(metaReadAddr_0)
  val metaReadTag_1 = cio.metaReadResp(1).tag
  val metaReadAddr_1 = Cat(metaReadTag_1, metaReadIndex, 0.U(OffsetBits.W))
  val metaReadNotMMIO_1 = !AddressSpace.isMMIO(metaReadAddr_1)
  val metaReadTag_2 = cio.metaReadResp(2).tag
  val metaReadAddr_2 = Cat(metaReadTag_2, metaReadIndex, 0.U(OffsetBits.W))
  val metaReadNotMMIO_2 = !AddressSpace.isMMIO(metaReadAddr_2)
  val metaReadTag_3 = cio.metaReadResp(3).tag
  val metaReadAddr_3 = Cat(metaReadTag_3, metaReadIndex, 0.U(OffsetBits.W))
  val metaReadNotMMIO_3 = !AddressSpace.isMMIO(metaReadAddr_3)
  val metaReadsNotMMIO = metaReadNotMMIO_0 & metaReadNotMMIO_1 & metaReadNotMMIO_2 & metaReadNotMMIO_3

  val metaWriteValid = cio.metaWriteBus.req.valid
  val metaWriteTag = cio.metaWriteBus.req.bits.data.tag
  val metaWriteIndex = cio.metaWriteBus.req.bits.setIdx
  val metaWriteAddr = Cat(metaWriteTag, metaWriteIndex, 0.U(OffsetBits.W))
  val metaWriteNotMMIO = !AddressSpace.isMMIO(metaWriteAddr) || !metaWriteValid

  // s2_goal_7: if mmio data will not be read or written to the cacheArray for k cyles from the beginning,
  //            then all valid mmio requsets will encounter a cache miss at the kth cyle
  // no_deps, can be proven
  chaAssert(this, "!((metaReadsNotMMIO && metaWriteNotMMIO) U (!s2ForwardingNotMMIO  && (metaReadsNotMMIO && metaWriteNotMMIO)))", true)
  
  //--- experimental ---/
  // chaAssert(this, "!((metaReadsNotMMIO && metaWriteNotMMIO) U (!s2ForwardingNotMMIO  && metaWriteNotMMIO ))", true)
  // chaAssert(this, " (s2ForwardingNotMMIO  || !metaReadsNotMMIO || !metaWriteNotMMIO)", true)
  //--- experimental ---/

  // --- another layer begins ---//
  // this assumption is conducted by some assertions in this stage before
  // s2_assume_1: mmio data will always not be read or written to the cacheArray
  // inducted by s2_goal_7, s3_goal_5, array_goal_3, induction by time
  // chaAssume(this, "metaReadsNotMMIO && metaWriteNotMMIO")

  val s3_mmio = s2.io.out.valid && s2.io.out.bits.mmio
  val s3_hit = s2.io.out.valid && s2.io.out.bits.hit
  
  // s2_goal_8: all mmio requsts will encounter a cache miss
  // depends on s2_assume_1, can be proven
  // chaAssert(this, "!s3_mmio || !s3_hit")
  // --- another layer ends ---//

  // s2_final_goal: request form s1 will trigger request to s3
  // no deps, can be proven
  chaAssert(this, "s1ReqValid -> F s2ReqValid")
}

class CacheStage3Prop(implicit val cacheConfig: CacheConfig) extends CacheModule{

  class Stage3PropIO extends Bundle {
    val in = Flipped(new SimpleBusUC(userBits = userBits, idBits = idBits))
    val out = new SimpleBusC
    val mmio = new SimpleBusUC

    val dataReadBus = CacheDataArrayReadBus()
    val dataWriteBus = CacheDataArrayWriteBus()
    val metaWriteBus = CacheMetaArrayWriteBus()
  }
  
  class PipeConnectIO extends Bundle {
    val in = Flipped(Decoupled(new Stage2IO))
  }

  val cio = IO(new Stage3PropIO)
  val pio = IO(new PipeConnectIO)

  val s3 = Module(new CacheStage3)
  PipelineConnect(pio.in, s3.io.in, s3.io.isFinish, false.asBool)
  
  // ----------------- connection ------------------
  cio.in.resp <> s3.io.out
  cio.out.mem <> s3.io.mem
  cio.mmio <> s3.io.mmio

  cio.out.coh := DontCare
  cio.in.req.ready := DontCare

  s3.io.cohResp.ready := true.B
  s3.io.flush := false.asBool
  s3.io.dataReadBus <> cio.dataReadBus
  s3.io.dataWriteBus <> cio.dataWriteBus
  s3.io.metaWriteBus <> cio.metaWriteBus

  // ----------------- assume and assertion ------------------

  val reqIsRead = pio.in.bits.req.cmd === SimpleBusCmd.read
  val reqIsWrite = s3.req.cmd === SimpleBusCmd.write
  
  val reqIsNotProbe = pio.in.bits.req.cmd =/= SimpleBusCmd.probe
  
  // s3_assume_0: the request is not a probe request
  assume(reqIsWrite)

  val memReqReady = cio.out.mem.req.ready
  // s3_assume_1: mem will utltimately be accessed
  // restriction for external signal
  chaAssume(this, "F memReqReady")

  val memReadFire = cio.out.mem.req.bits.cmd === SimpleBusCmd.readBurst & cio.out.mem.req.fire()
  val memRespVal = cio.out.mem.resp.valid
  val memRespFin = cio.out.mem.resp.bits.isReadLast()
  // s3_assume_2: handshaking of read request to mem will utltimately be responsed and handled 
  // restriction for external signal
  chaAssume(this, "memReadFire -> X F (memRespVal U (memRespVal && memRespFin))")

  val memWriteFire = cio.out.mem.req.bits.cmd === SimpleBusCmd.writeLast & cio.out.mem.req.fire()
  // s3_assume_3: handshaking of write request to mem will utltimately be responsed 
  // restriction for external signal
  chaAssume(this, "memWriteFire -> X F memRespVal")

  val s3RespReady = cio.in.resp.ready
  // s3_assume_4: handshaking of read request to mem will utltimately be responsed and handled 
  // restriction for external signal
  chaAssume(this, "F s3RespReady")

  val s3RespFire = cio.in.resp.fire()
  // s3_assume_5: mem will utltimately be accessed
  // restriction for external
  chaAssume(this, "s3RespReady -> X((s3RespReady U s3RespFire) || G s3RespReady)")

  val mmioReqReady = cio.mmio.req.ready
  // s3_assume_6: mmio will utltimately be accessed
  // restriction for external signal
  chaAssume(this, "F mmioReqReady")

  val mmioReqFire = cio.mmio.req.fire()
  val mmioRespVal = cio.mmio.resp.valid
  // s3_assume_7: handshaking of read request to mmio will utltimately be responsed and handled 
  // restriction for external signal
  chaAssume(this, "mmioReqFire -> X F F mmioRespVal ")

  val s2ReqValid = pio.in.valid
  val s2ReqFire = pio.in.fire
  // s3_assume_8 : request to s3 will keep high until getting handshake
  // verified by s2_goal_2: note that it is an overlapping implication
  chaAssume(this, "s2ReqValid -> ((s2ReqValid U s2ReqFire) || G s2ReqValid)")

  val s3ReqValid = cio.in.resp.valid
  val dataArrayReadReady = cio.dataReadBus.req.ready
  // s3_assume_9 : if no request to CPU, then metaArray and dataArray can eventually be accessed 
  // inducted by s3_goal_3, s2_goal_4, s1_goal_1, s3_goal_4, array_goal_2
  chaAssume(this, "(G !s3ReqValid) -> F G dataArrayReadReady")
  
  val s2ForwardingMetas = pio.in.bits.metas
  val s2ForwardingWayMask = pio.in.bits.waymask
  val s2ForwardingTag = Mux1H(s2ForwardingWayMask, s2ForwardingMetas).tag
  val s2ValidHit = pio.in.valid && pio.in.bits.hit
  val reqTag = pio.in.bits.req.addr.asTypeOf(addrBundle).tag
  val consistIndex = !s2ValidHit || (s2ForwardingTag === reqTag)
  // s3_assume_10: if request to s3 is valid and the request meets a hit, then the output tag is the same as its in request 
  // verified in s2_goal_5
  chaAssume(this, "consistIndex")
  
  val s3IsMmio = pio.in.bits.mmio && pio.in.valid
  val reqIsMmio = AddressSpace.isMMIO(pio.in.bits.req.addr) && pio.in.valid
  // s3_assume_11: s2 will set the mmio-bit true <=> the request is a mmio request  
  // verified in s2_goal_6
  chaAssume(this, "(!s3IsMmio || reqIsMmio) && (!reqIsMmio || s3IsMmio)")

  val arrayWriteReqValid = cio.metaWriteBus.req.valid || cio.dataWriteBus.req.valid
  // s3_goal_1: if no request from s2, there will be no request to write metaArray and dataArray
  // depend on s3_assume_1, s3_assume_2, s3_assume_4: partially ensure the run of state mechine, can be proven
  // chaAssert(this, "(G !s2ReqValid) -> F G !arrayWriteReqValid")

  val s2ReqReady = pio.in.ready
  // s3_goal_2: if s3 is always unready, there will be no request to write metaArray and dataArray
  // depend on: s3_assume_1 ~ s3_assume_4, s3_assume_6, s3_assume_8, can be proven
  // chaAssert(this, "(G !s2ReqReady) -> F G !arrayWriteReqValid")
  
  // s3_goal_3: if no response to cpu, s2 and s3 will no be able to make handshake eventually
  // take all the assumptions, can be proven
  // chaAssert(this, "(G !s3ReqValid) -> F G !s2ReqFire")

  // s3_goal_4: if no response to cpu, there will be no request to write metaArray and dataArray
  // take all the assumptions, can be proven
  // chaAssert(this, "(G !s3ReqValid) -> F G !arrayWriteReqValid")

  val s2ForwardingNotMMIO = !AddressSpace.isMMIO(pio.in.bits.req.addr) || !s2ValidHit
  val metaWriteValid = cio.metaWriteBus.req.valid
  val metaWriteTag = cio.metaWriteBus.req.bits.data.tag
  val metaWriteIndex = cio.metaWriteBus.req.bits.setIdx
  val metaWriteAddr = Cat(metaWriteTag, metaWriteIndex, 0.U(OffsetBits.W))
  val metaWriteNotMMIO = !AddressSpace.isMMIO(metaWriteAddr) || !metaWriteValid

  // s3_goal_5: if all valid mmio requsets will encounter a cache miss  for k cyles from the beginning,
  //            then  mmio data will not be written to the cacheArray at the kth cyle
  // chaAssert(this, "!(s2ForwardingNotMMIO U ! metaWriteNotMMIO)", true)

  // --- another layer begins ---//
  // this assumption is conducted by some assertions in this stage before
  val s3_mmio = pio.in.valid && pio.in.bits.mmio
  val s3_hit = pio.in.valid && pio.in.bits.hit
  // s3_assume_12: all mmio requsts will encounter a cache miss
  // verified in s2_goal_8
  chaAssume(this, "!s3_mmio || !s3_hit")
  
  // s3_final_goal: request form s2 will trigger response to cpu
  // depend on the all assume statement (in)directly
  chaAssert(this, "s2ReqValid -> F s3ReqValid")
  // --- another layer ends ---//
}

class CacheArrayProp(implicit val cacheConfig: CacheConfig) extends CacheModule{

  class CacheArrayPropIO extends Bundle {
    val s1_metaReadBus = Flipped(CacheMetaArrayReadBus())
    val s1_dataReadBus = Flipped(CacheDataArrayReadBus())

    val s3_metaWriteBus = Flipped(CacheMetaArrayWriteBus())
    val s3_dataReadBus = Flipped(CacheDataArrayReadBus())
    val s3_dataWriteBus = Flipped(CacheDataArrayWriteBus())
  }    

  val cio = IO(new CacheArrayPropIO)

  val metaArray = Module(new SRAMTemplateWithArbiter(nRead = 1, new MetaBundle, set = Sets, way = Ways, shouldReset = true))
  val dataArray = Module(new SRAMTemplateWithArbiter(nRead = 2, new DataBundle, set = Sets * LineBeats, way = Ways))

  metaArray.io.r(0) <> cio.s1_metaReadBus
  dataArray.io.r(0) <> cio.s1_dataReadBus
  dataArray.io.r(1) <> cio.s3_dataReadBus

  metaArray.io.w <> cio.s3_metaWriteBus
  dataArray.io.w <> cio.s3_dataWriteBus

  // ----------------- assume and assertion ------------------

  val arrayWriteReqValid = cio.s3_metaWriteBus.req.valid || cio.s3_dataWriteBus.req.valid
  val arrayReadReqReady = cio.s1_metaReadBus.req.ready & cio.s1_dataReadBus.req.ready
  // array_goal_1: if there is always no write request to array,  array will be ready to read eventually
  // no deps, can be proven 
  // chaAssert(this, "(G !arrayWriteReqValid) -> F G arrayReadReqReady")

  val dataArrayReadValid = cio.s1_dataReadBus.req.valid
  val dataArrayReadReady = cio.s3_dataReadBus.req.ready
  // array_goal_2: 
  // no deps, can be proven 
  // chaAssert(this, "(G !dataArrayReadValid & !arrayWriteReqValid) -> F G dataArrayReadReady")

  val metaWriteValid = cio.s3_metaWriteBus.req.valid
  val metaWriteTag = cio.s3_metaWriteBus.req.bits.data.tag
  val metaWriteIndex = cio.s3_metaWriteBus.req.bits.setIdx
  val metaWriteAddr = Cat(metaWriteTag, metaWriteIndex, 0.U(OffsetBits.W))
  val metaWriteNotMMIO = !AddressSpace.isMMIO(metaWriteAddr) || !metaWriteValid

  val metaReadReady = cio.s1_metaReadBus.req.ready
  // not restrict index, but have no effect
  val metaReadIndex = cio.s1_metaReadBus.req.bits.setIdx
  val metaReadTag_0 = cio.s1_metaReadBus.resp.data(0).tag
  val metaReadAddr_0 = Cat(metaReadTag_0, metaReadIndex, 0.U(OffsetBits.W))
  val metaReadNotMMIO_0 = !AddressSpace.isMMIO(metaReadAddr_0)

  val metaReadTag_1 = cio.s1_metaReadBus.resp.data(1).tag
  val metaReadAddr_1 = Cat(metaReadTag_1, metaReadIndex, 0.U(OffsetBits.W))
  val metaReadNotMMIO_1 = !AddressSpace.isMMIO(metaReadAddr_1)

  val metaReadTag_2 = cio.s1_metaReadBus.resp.data(2).tag
  val metaReadAddr_2 = Cat(metaReadTag_2, metaReadIndex, 0.U(OffsetBits.W))
  val metaReadNotMMIO_2 = !AddressSpace.isMMIO(metaReadAddr_2)

  val metaReadTag_3 = cio.s1_metaReadBus.resp.data(3).tag
  val metaReadAddr_3 = Cat(metaReadTag_3, metaReadIndex, 0.U(OffsetBits.W))
  val metaReadNotMMIO_3 = !AddressSpace.isMMIO(metaReadAddr_3)
  val metaReadsNotMMIO = metaReadNotMMIO_0 & metaReadNotMMIO_1 & metaReadNotMMIO_2 & metaReadNotMMIO_3

  // array_goal_3: if input follows that there is no write with mmio addr, then the result of read request is not about mmio
  // no deps, can be proven ()
  chaAssert(this, "!(metaWriteNotMMIO U !metaReadsNotMMIO)", true)

  // --- modified by yusz
  // dontTouch(metaWriteNotMMIO)
  // val aa = Wire(Vec(Sets, Vec(Ways, new MetaBundle)))
  // aa := DontCare
  // BoringUtils.bore(metaArray.ram.accessArray, Seq(aa))
  // val bb = VecInit(aa.flatten.map{x:MetaBundle =>  !AddressSpace.isMMIO(Cat(x.tag, metaReadIndex, 0.U(OffsetBits.W)))}).asUInt.andR
  // chaAssert(this, "!metaWriteNotMMIO || ! X bb || ! metaReadsNotMMIO || X metaReadsNotMMIO")

  // chaAssert(this, "! metaWriteNotMMIO || ! X bb || X X bb")
  // chaAssert(this, "metaReadsNotMMIO && X metaReadsNotMMIO && X X metaReadsNotMMIO && X X X metaReadsNotMMIO", true)

  // --- modification ends

  // val temp2 = Cat(temp, metaReadIndex, 0.U(OffsetBits.W))
  // val temp3 = !AddressSpace.isMMIO(temp2)
  // // array_goal_4
  // chaAssert(this, "!(metaWriteNotMMIO U !temp3)", true)

}

class CachePropSpec extends AnyFlatSpec with ChiselScalatestTester with Formal {

  // println(new (chisel3.stage.ChiselStage).emitSystemVerilog(new CacheStage1Prop()))
  
  // behavior of "CacheStage1"
  // it should "pass" in {
  //   verify(new CacheStage1Prop()(new CacheConfig()), Seq(Ic3SaCheck(20), PonoEngineAnnotation))
  // }

  // behavior of "CacheStage2"
  // it should "pass" in {
  //   verify(new CacheStage2Prop()(new CacheConfig()), Seq(Ic3SaCheck(50), PonoEngineAnnotation))
  // }

  behavior of "CacheStage3"
  it should "pass" in {
    verify(new CacheStage3Prop()(new CacheConfig()), Seq(Ic3SaCheck(50), PonoEngineAnnotation, EnSafetyOpti))
    // verify(new CacheStage3Prop()(new CacheConfig()), Seq(Ic3SaCheck(50), PonoEngineAnnotation, EnSafetyOpti))
    // verify(new CacheStage3Prop, Seq(Ic3SaCheck(50), PonoEngineAnnotation))
  }
  //   verify(new CacheStage3Prop()(new CacheConfig()), Seq(BoundedCheck(25), PonoEngineAnnotation))
  //   // verify(new CacheStage3Prop()(new CacheConfig()), Seq(Ic3SaCheck(50), PonoEngineAnnotation, EnSafetyOpti))
  //   // verify(new CacheStage3Prop, Seq(Ic3SaCheck(50), PonoEngineAnnotation))
  // }

  // println(new (chisel3.stage.ChiselStage).emitSystemVerilog(new Ic3SaCheck()(new CacheConfig())))
  // behavior of "CacheStage3"
  // it should "pass" in {
  //   verify(new CacheStage3Prop()(new CacheConfig()), Seq(BoundedCheck(25), PonoEngineAnnotation))
  //   // verify(new CacheStage3Prop()(new CacheConfig()), Seq(Ic3SaCheck(50), PonoEngineAnnotation, EnSafetyOpti))
  //   // verify(new CacheStage3Prop, Seq(Ic3SaCheck(50), PonoEngineAnnotation))
  // }

  // behavior of "CacheArray"
  // it should "pass" in {
  //   verify(new CacheArrayProp()(new CacheConfig()), Seq(BoundedCheck(100), PonoEngineAnnotation, EnSafetyOpti))
  // }
}