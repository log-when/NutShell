// package testCHA.CacheTest

// import org.scalatest.flatspec._
// import org.scalatest.matchers.should._
// import chiseltest._
// import chiseltest.formal._
// import chiseltest.formal.chaAnno._
// import chisel3._
// import nutcore._
// import nutcore.CacheConfig
// import utils._

// import chisel3.util._
// import chisel3.util.experimental.BoringUtils
// import bus.axi4._
// import chisel3.experimental.IO

// import chisel3.experimental.{ChiselAnnotation, annotate}
// import firrtl.annotations.{Annotation, MemoryArrayInitAnnotation, MemoryScalarInitAnnotation, ReferenceTarget}


// import bus.simplebus._
// // class CacheStage1Prop extends CacheStage1()(CacheConfig()){
  
// //   //
// //   val coreReq = io.in.valid
// //   val s1Free = io.in.ready

// //   val metaReadReady = io.metaReadBus.req.ready
// //   val dataReadReady = io.dataReadBus.req.ready
  
// //   val s2Ready = io.out.ready
// //   val handShakingS2 = io.out.fire()

// //   // request form core will keep high unless being responsed
// //   // chaAssume(this,"coreReq |-> (G coreReq) || (coreReq U coreResp)")
// //   // suppose cacheStage2 will eventually be available
// //   chaAssume(this,"F s2Ready")
// //   // request for metaArray and dataArray will be responsed simultaneously
// //   // the reason is s1 has higher priority when reading metadata
// //   chaAssume(this,"F (metaReadReady && dataReadReady)")
  
// //   // to verify that request will always be responsed ultimately
// //   chaAssert(this,"F s1Free")
// // }



// // -until_with is neccessary
// // class CacheStage1Prop extends CacheStage1()(CacheConfig()){
  
// //   val coreReq = io.in.valid
// //   val s1Req = io.out.valid
// //   val n_s1Req = !s1Req

// //   val metaReadReady = io.metaReadBus.req.ready
// //   val dataReadReady = io.dataReadBus.req.ready
  
// //   // request form core will keep high unless being responsed
// //   chaAssume(this,"coreReq |-> ((G coreReq) || (coreReq U s1Req))")
// //   //  will be available 
// //   chaAssume(this,"n_s1Req |-> F (metaReadReady && dataReadReady)")
  
// //   // to verify that request will always be responsed ultimately
// //   chaAssert(this,"coreReq |-> F s1Req")
// // }

// // class CacheStage2Prop extends CacheStage2()(CacheConfig()){
  
// //   class PipeConnectIO extends Bundle {
// //     val in = Flipped(Decoupled(new Stage1IO))
// //   }
// //   val pio = IO(new PipeConnectIO)


// //   val out = Decoupled(new Stage1IO)
// //   PipelineConnect(pio.in, io.in, io.out.fire(), false.asBool)
// //   val s1Req = io.in.valid
// //   val s2Req = io.out.valid
// //   val s1Resp = io.in.ready
// //   val s3Ready = io.out.ready

// //   // assert(s1Req)
// //   // chaAssume(this,"F temp")
// //   // chaAssume()
// //   // to verify that request will always be responsed ultimately
// //   chaAssert(this,"s1Req |-> F s2Req")
// // }

// class CacheStage2Prop(implicit val cacheConfig: CacheConfig) extends CacheModule{
  
//   class Stage2PropIO extends Bundle {
//     val metaReadResp = Flipped(Vec(Ways, new MetaBundle))
//     val dataReadResp = Flipped(Vec(Ways, new DataBundle))
//     val metaWriteBus = Input(CacheMetaArrayWriteBus())
//     val dataWriteBus = Input(CacheDataArrayWriteBus())
//     // val out = Decoupled(new Stage2IO)
//   }

//   class PipeConnectIO extends Bundle {
//     val in = Flipped(Decoupled(new Stage1IO))
//   }

//   val cio = IO(new Stage2PropIO)
//   val pio = IO(new PipeConnectIO)

//   val s2 = Module(new CacheStage2)
//   PipelineConnect(pio.in, s2.io.in, s2.io.out.fire(), false.asBool)

//   s2.io.metaReadResp := cio.metaReadResp
//   s2.io.dataReadResp := cio.dataReadResp
//   s2.io.dataWriteBus := cio.dataWriteBus
//   s2.io.metaWriteBus := cio.metaWriteBus

//   s2.io.out.ready := DontCare

//   // cio.out := s2.io.out
//   val s1ReqValid = pio.in.valid
//   val s1ReqFire = pio.in.fire()
//   val s2ReqValid = s2.io.out.valid
//   val s2ReqFire = s2.io.out.fire()
  
//   // val s2ReqHit = s2.io.out.bits.hit
//   // val s2ReqMMio = s2.io.out.bits.mmio

//   // val ob_s2ReqHit = RegNext(s2ReqHit)
//   // dontTouch(ob_s2ReqHit)
//   // val ob_s2ReqMMio = RegNext(s2ReqMMio)
//   // dontTouch(ob_s2ReqMMio)
//   // val ob_s2OutValid = RegNext(s2.io.out.valid)
//   // dontTouch(ob_s2OutValid)
//   // val ob_s2OutFire = RegNext(s2.io.out.fire())
//   // dontTouch(ob_s2OutFire)
    
//   // assert(!s2ReqHit || !s2ReqMMio)


//   // s3_a1: no need for assumption, guaranteed by pipeConnect between s1 and s2, can be proven
//   chaAssert(this, "s2ReqValid |-> ((s2ReqValid U s2ReqFire) || G s2ReqValid)")
//   // s2_aim: no need for assumption, can be proven
//   chaAssert(this, "s1ReqValid |=> F s2ReqValid")
// }

// class CacheStage3Prop(implicit val cacheConfig: CacheConfig) extends CacheModule{

//   class Stage3PropIO extends Bundle {
//     val in = Flipped(new SimpleBusUC(userBits = userBits, idBits = idBits))
//     val out = new SimpleBusC
//     val mmio = new SimpleBusUC

//     val dataReadBus = CacheDataArrayReadBus()
//     val dataWriteBus = CacheDataArrayWriteBus()
//     val metaWriteBus = CacheMetaArrayWriteBus()
//   }
  
//   class PipeConnectIO extends Bundle {
//     val in = Flipped(Decoupled(new Stage2IO))
//   }

//   val cio = IO(new Stage3PropIO)
//   val pio = IO(new PipeConnectIO)

//   val s3 = Module(new CacheStage3)
//   PipelineConnect(pio.in, s3.io.in, s3.io.isFinish, false.asBool)
  
//   cio.in.resp <> s3.io.out
//   cio.out.mem <> s3.io.mem
//   cio.mmio <> s3.io.mmio

//   cio.out.coh := DontCare
//   cio.in.req.ready := DontCare

//   s3.io.cohResp.ready := true.B
//   s3.io.flush := false.asBool
//   s3.io.dataReadBus <> cio.dataReadBus
//   s3.io.dataWriteBus <> cio.dataWriteBus
//   s3.io.metaWriteBus <> cio.metaWriteBus

//   val s2ReqValid = pio.in.valid
//   val ns2ReqValid = !s2ReqValid
//   val s2ReqFire = pio.in.fire
  
//   // s3_a1 : note that it is an overlapping implication
//   chaAssume(this, "s2ReqValid |-> ((s2ReqValid U s2ReqFire) || G s2ReqValid)")

//   val reqIsRead = s3.req.cmd === SimpleBusCmd.read

//   // cio.in.resp  <> s3.io.out
//   val s3ReqReady = cio.in.resp.ready
//   val s3ReqValid = cio.in.resp.valid
//   val s3RespFire = cio.in.resp.fire()

//   //cpu will be accessible
//   chaAssume(this, "F s3ReqReady")
//   chaAssume(this, "s3ReqReady |-> X((s3ReqReady U s3RespFire) || G s3ReqReady)")


//   val dataReadReqRea = cio.dataReadBus.req.ready

//   chaAssume(this, "F dataReadReqRea")

//   val memReqRea = cio.out.mem.req.ready
//   val memRespVal = cio.out.mem.resp.valid
  
//   val memReadFire = cio.out.mem.req.bits.cmd === SimpleBusCmd.readBurst & cio.out.mem.req.valid & cio.out.mem.req.ready
//   val memReadFin = cio.out.mem.resp.bits.isReadLast
//   val memWriteFire = cio.out.mem.req.bits.cmd === SimpleBusCmd.writeLast & cio.out.mem.req.valid & cio.out.mem.req.ready
//   // val memWriteFin = cio.out.mem.resp.fire
  
//   chaAssume(this, "F memReqRea")
//   // : read request to memory will be responsed 
//   chaAssume(this, "memReadFire |=> F(memRespVal U (memRespVal && memReadFin))")
//   // chaAssume(this, "memReadFire |=> F memRespVal")
//   // : write request to memory will be responsed 
//   chaAssume(this, "memWriteFire |=> F memRespVal")
  


//   val mmioReqRea = cio.mmio.req.ready
//   val mmioReqFire = cio.mmio.req.valid & cio.mmio.req.ready
//   val mmioRespVal = cio.mmio.resp.valid
//   // val s2ReqReady = s3.io.in.ready
  
//   chaAssume(this, "F mmioReqRea")
//   chaAssume(this, "mmioReqFire |=> F mmioRespVal ")



//   val ob_memIsWriteLast = RegNext(cio.out.mem.req.bits.isWriteLast()) 
//   val ob_memReqFire = RegNext(cio.out.mem.req.fire())
//   dontTouch(ob_memIsWriteLast)
//   dontTouch(ob_memReqFire)
  

//   assume(!pio.in.bits.mmio || !pio.in.bits.hit)
//   // chaAssume(!)
//   assume(reqIsRead)
  

//   //

//   val s2ForwardingValid = pio.in.valid
//   val s2ForwardingMetas = pio.in.bits.metas
//   val s2ForwardingWayMask = pio.in.bits.waymask
//   val s2ForwardingTag = Mux1H(s2ForwardingWayMask, s2ForwardingMetas).tag
//   val reqTag = pio.in.bits.req.addr.asTypeOf(addrBundle).tag
//   val consistIndex = s2ForwardingTag === reqTag 
//   // to be proven
//   chaAssume(this, "consistIndex")
//   // val s2ForwardingIndex = pio.in.bits.req.addr

//   val s2ForwardingNotMMIO = !AddressSpace.isMMIO(pio.in.bits.req.addr) || !s2ForwardingValid

//   val ob_temp = RegNext(s2ForwardingNotMMIO)
//   dontTouch(ob_temp)
//   // to be proven
//   // chaAssume(this, "s2ForwardingNotMMIO")

//   // val s2ForwardingValid = pio.in.valid
//   // val s2ForwardingMetas = pio.in.bits.metas
//   // val s2ForwardingWayMask = pio.in.bits.waymask
//   // val s2ForwardingTag = Mux1H(s2ForwardingWayMask, s2ForwardingMetas).tag
//   // // val s2ForwardingIndex = pio.in.bits.req.addr


//   // val s2ForwardingAddr = Cat(s2ForwardingTag, s2ForwardingIndex, 0.U(OffsetBits.W))
//   // val s2ForwardingNotMMIO = !AddressSpace.isMMIO(metaWriteAddr) || !metaWriteValid
//   // chaAssume(this, "s2ForwardingNotMMIO")



//   val metaWriteValid = cio.metaWriteBus.req.valid
//   val metaWriteTag = cio.metaWriteBus.req.bits.data.tag
//   val metaWriteIndex = cio.metaWriteBus.req.bits.setIdx
//   val metaWriteAddr = Cat(metaWriteTag, metaWriteIndex, 0.U(OffsetBits.W))
//   val metaWriteNotMMIO = !AddressSpace.isMMIO(metaWriteAddr) || !metaWriteValid
  
//   val ob_metaWriteValid = RegNext(metaWriteValid)
//   val ob_metaWriteIndex = RegNext(metaWriteIndex)
//   val ob_metaWriteTag = RegNext(metaWriteTag)
//   val ob_metaWriteMMIO = RegNext(AddressSpace.isMMIO(metaWriteAddr))
//   dontTouch(ob_metaWriteValid)
//   dontTouch(ob_metaWriteIndex)
//   dontTouch(ob_metaWriteTag)
//   dontTouch(ob_metaWriteMMIO)

//   // chaAssert(this, "metaWriteNotMMIO")
  
//   val ob_s3InValid = RegNext(s3.io.in.valid)
//   dontTouch(ob_s3InValid)
//   val ob_s3InReady = RegNext(s3.io.in.ready)
//   dontTouch(ob_s3InReady)

//   chaAssert(this, "!(s2ForwardingNotMMIO U ! metaWriteNotMMIO)")
//   // chaAssert(this, "(ns2ReqValid ##1 s2ReqValid) |-> F s3ReqValid")
//   // assert(s3ReqValid)
//   // val s2ReqReady = io.in.ready
//   // val s2ReqValid = io.in.valid
//   // val s3ReqValid = io.out.valid
//   // val reqIsRead = req.cmd === SimpleBusCmd.read
//   // val reqIsWrite = req.cmd === SimpleBusCmd.write
//   // val reqIsreadBurst = req.cmd === SimpleBusCmd.readBurst
//   // val reqIsWriteBurst = req.cmd === SimpleBusCmd.writeBurst
//   // val reqIsPrefetch = req.cmd === SimpleBusCmd.prefetch


//   // assume(!io.flush)
//   // assume(reqIsWrite)
  
//   // // chaAssume("this, reqIsRead |-> reqIsRead U ")
//   // chaAssume(this, "F s3ReqReady")
//   // chaAssume(this, "s3ReqReady |-> X((s3ReqReady U s3ReqValid) || G s3ReqReady)")

//   // val memReqVal = io.mem.req.valid
//   // val memReqCmd = io.mem.req.bits.cmd
//   // val memReqRea = io.mem.req.ready
//   // val memReqFire = io.mem.req.valid & io.mem.req.ready

//   // val memReadFire = memReqCmd === SimpleBusCmd.readBurst & io.mem.req.valid & io.mem.req.ready
//   // val memWriteFire = memReqCmd === SimpleBusCmd.writeLast & io.mem.req.valid & io.mem.req.ready

//   // val memRespVal = io.mem.resp.valid
//   // val memReadFin = io.mem.resp.bits.isReadLast
//   // val memWriteFin = io.mem.req.bits.isWriteLast

//   // val temp = io.dataReadBus.req.ready
//   // val mmioReqRea = io.mmio.req.ready
//   // val mmioReqFire = io.mmio.req.valid & io.mmio.req.ready
//   // val mmioRespVal = io.mmio.resp.valid

//   // val mmioReq = io.in.valid & io.in.bits.mmio
//   // val nMmioReq = io.in.valid & !io.in.bits.mmio
  
//   // chaAssume(this, "mmioReq |-> X((mmioReq U s2ReqReady) || G mmioReq)")
//   // chaAssume(this, "nMmioReq |-> X((nMmioReq U s2ReqReady) || G nMmioReq)")

//   // val hitReq = io.in.valid & io.in.bits.hit
//   // val nhitReq = io.in.valid & !io.in.bits.hit

//   // chaAssume(this, "hitReq |-> X hitReq U s2ReqReady || G hitReq")
//   // chaAssume(this, "nhitReq |-> X((nhitReq U s2ReqReady) || G nhitReq)")

//   // val isFinish = io.isFinish
//   // val ns2ReqReady = !s2ReqReady
//   // chaAssume(this, "s2ReqValid |-> (s2ReqValid U (s2ReqValid && isFinish && !s2ReqReady))")
//   // chaAssume(this, "(isFinish && !s2ReqReady) -> X !s2ReqValid")

//   // chaAssume(this, "F temp")
//   // chaAssume(this, "F memReqRea")
//   // chaAssume(this, "F mmioReqRea")
  
//   // chaAssume(this, "mmioReqFire |-> X (F mmioRespVal) ")
//   // chaAssume(this, "memReadFire |-> X (F(memRespVal U (memRespVal && memReadFin)))")
//   // chaAssume(this, "memWriteFire |-> X (F(memReqRea U (memReqRea && memWriteFin)))")

//   // //if memory is ready, it will keep ready until receiving request
//   // chaAssume(this, "memReqRea |-> X((memReqRea U memReqFire) || G memReqRea)")

//   // val ns2ReqValid = !s2ReqValid
//   // chaAssert(this, "(ns2ReqValid ##1 s2ReqValid) |-> F s3ReqValid")

  
// }

// class CacheArrayProp(implicit val cacheConfig: CacheConfig) extends CacheModule{

//   // def annoMem(a: ReferenceTarget => Annotation): Unit = {
//   //   annotate(new ChiselAnnotation {
//   //     override def toFirrtl = a(metaArray.ram.array.toTarget)
//   //   })
//   // }

//   // annoMem(MemoryArrayInitAnnotation(_, Seq.fill(21)(0)))

//   val metaArray = Module(new SRAMTemplateWithArbiter(nRead = 1, new MetaBundle, set = Sets, way = Ways, shouldReset = true))
//   val dataArray = Module(new SRAMTemplateWithArbiter(nRead = 2, new DataBundle, set = Sets * LineBeats, way = Ways))

//   class CacheArrayPropIO extends Bundle {
//     val s1_metaReadBus = Flipped(CacheMetaArrayReadBus())
//     val s1_dataReadBus = Flipped(CacheDataArrayReadBus())

//     val s3_metaWriteBus = Flipped(CacheMetaArrayWriteBus())
//     val s3_dataReadBus = Flipped(CacheDataArrayReadBus())
//     val s3_dataWriteBus = Flipped(CacheDataArrayWriteBus())
//   }    


//   val cio = IO(new CacheArrayPropIO)

//   metaArray.io.r(0) <> cio.s1_metaReadBus
//   dataArray.io.r(0) <> cio.s1_dataReadBus
//   dataArray.io.r(1) <> cio.s3_dataReadBus

//   metaArray.io.w <> cio.s3_metaWriteBus
//   dataArray.io.w <> cio.s3_dataWriteBus


//   // val metaReadReady = metaArray.io.r(0).req.ready
//   // val testValid = !metaReadReady
//   // // val testValid = !metaArray.io.r(0).resp.data(0).valid && !metaReadReady
//   // val ob_testValid = RegNext(testValid)
//   // dontTouch(ob_testValid)

//   // chaAssume(this, "")
//   val metaWriteValid = cio.s3_metaWriteBus.req.valid
//   val metaWriteTag = cio.s3_metaWriteBus.req.bits.data.tag
//   val metaWriteIndex = cio.s3_metaWriteBus.req.bits.setIdx
//   val metaWriteAddr = Cat(metaWriteTag, metaWriteIndex, 0.U(OffsetBits.W))
//   val metaWriteNotMMIO = !AddressSpace.isMMIO(metaWriteAddr) || !metaWriteValid

  
//   val metaReadReady = cio.s1_metaReadBus.req.ready
//   // not restrict index, but have no effect
//   val metaReadIndex = cio.s1_metaReadBus.req.bits.setIdx
//   // val metaReadValid = cio.s1_metaReadBus.resp.valid
//   val metaReadTag_0 = cio.s1_metaReadBus.resp.data(0).tag
//   val metaReadAddr_0 = Cat(metaReadTag_0, metaReadIndex, 0.U(OffsetBits.W))
//   val metaReadNotMMIO_0 = !AddressSpace.isMMIO(metaReadAddr_0)

//   val metaReadTag_1 = cio.s1_metaReadBus.resp.data(1).tag
//   val metaReadAddr_1 = Cat(metaReadTag_1, metaReadIndex, 0.U(OffsetBits.W))
//   val metaReadNotMMIO_1 = !AddressSpace.isMMIO(metaReadAddr_1)

//   val metaReadTag_2 = cio.s1_metaReadBus.resp.data(2).tag
//   val metaReadAddr_2 = Cat(metaReadTag_2, metaReadIndex, 0.U(OffsetBits.W))
//   val metaReadNotMMIO_2 = !AddressSpace.isMMIO(metaReadAddr_2)

//   val metaReadTag_3 = cio.s1_metaReadBus.resp.data(3).tag
//   val metaReadAddr_3 = Cat(metaReadTag_3, metaReadIndex, 0.U(OffsetBits.W))
//   val metaReadNotMMIO_3 = !AddressSpace.isMMIO(metaReadAddr_3)

//   val metaReadsNotMMIO = metaReadNotMMIO_0 & metaReadNotMMIO_1 & metaReadNotMMIO_2 & metaReadNotMMIO_3
  
//   // val initPartMetaArray = metaArray.ram.array(3).rdata_MPORT_data === 0.U(PAddrBits.W) && 
//   // chaAssume(this, "initPartMetaArray")
//   chaAssert(this, "!(metaWriteNotMMIO U ! (!metaReadReady || metaReadsNotMMIO))")
//   // chaAssert(this, "F testValid")
// }

// // class CacheStage2Prop extends CacheStage2()(CacheConfig()){
  
// //   val s1Req = io.in.valid
// //   val s2Req = io.out.valid
// //   val s1Resp = io.in.ready
// //   val s3Ready = io.out.ready

// //   // assert(s1Req)
// //   // chaAssume(this,"F temp")
// //   // chaAssume()
// //   // to verify that request will always be responsed ultimately
// //   chaAssert(this,"s1Req |-> F s2Req")
// // }

// // class CacheStage3Prop extends CacheStage3()(CacheConfig()){

// //   val s2ReqReady = io.in.ready
// //   val s2ReqValid = io.in.valid
// //   val s3ReqValid = io.out.valid
// //   val reqIsRead = req.cmd === SimpleBusCmd.read
// //   val reqIsWrite = req.cmd === SimpleBusCmd.write
// //   val reqIsreadBurst = req.cmd === SimpleBusCmd.readBurst
// //   val reqIsWriteBurst = req.cmd === SimpleBusCmd.writeBurst
// //   val reqIsPrefetch = req.cmd === SimpleBusCmd.prefetch
// //   val s3ReqReady = io.out.ready

// //   assume(!io.flush)
// //   assume(reqIsWrite)
  
// //   // chaAssume("this, reqIsRead |-> reqIsRead U ")
// //   chaAssume(this, "F s3ReqReady")
// //   chaAssume(this, "s3ReqReady |-> X((s3ReqReady U s3ReqValid) || G s3ReqReady)")

// //   val memReqVal = io.mem.req.valid
// //   val memReqCmd = io.mem.req.bits.cmd
// //   val memReqRea = io.mem.req.ready
// //   val memReqFire = io.mem.req.valid & io.mem.req.ready

// //   val memReadFire = memReqCmd === SimpleBusCmd.readBurst & io.mem.req.valid & io.mem.req.ready
// //   val memWriteFire = memReqCmd === SimpleBusCmd.writeLast & io.mem.req.valid & io.mem.req.ready

// //   val memRespVal = io.mem.resp.valid

// //   val memReadFin = io.mem.resp.bits.isReadLast

  
// //   val memWriteFin = io.mem.req.bits.isWriteLast

// //   val temp = io.dataReadBus.req.ready
// //   val mmioReqRea = io.mmio.req.ready
// //   val mmioReqFire = io.mmio.req.valid & io.mmio.req.ready
// //   val mmioRespVal = io.mmio.resp.valid

// //   val mmioReq = io.in.valid & io.in.bits.mmio
// //   val nMmioReq = io.in.valid & !io.in.bits.mmio
  
// //   chaAssume(this, "mmioReq |-> X((mmioReq U s2ReqReady) || G mmioReq)")
// //   chaAssume(this, "nMmioReq |-> X((nMmioReq U s2ReqReady) || G nMmioReq)")

// //   val hitReq = io.in.valid & io.in.bits.hit
// //   val nhitReq = io.in.valid & !io.in.bits.hit

// //   chaAssume(this, "hitReq |-> X hitReq U s2ReqReady || G hitReq")
// //   chaAssume(this, "nhitReq |-> X((nhitReq U s2ReqReady) || G nhitReq)")

// //   val isFinish = io.isFinish
// //   val ns2ReqReady = !s2ReqReady
// //   chaAssume(this, "s2ReqValid |-> (s2ReqValid U (s2ReqValid && isFinish && !s2ReqReady))")
// //   chaAssume(this, "(isFinish && !s2ReqReady) -> X !s2ReqValid")

// //   chaAssume(this, "F temp")
// //   chaAssume(this, "F memReqRea")
// //   chaAssume(this, "F mmioReqRea")
  
// //   chaAssume(this, "mmioReqFire |-> X (F mmioRespVal) ")
// //   chaAssume(this, "memReadFire |-> X (F(memRespVal U (memRespVal && memReadFin)))")
// //   chaAssume(this, "memWriteFire |-> X (F(memReqRea U (memReqRea && memWriteFin)))")

// //   //if memory is ready, it will keep ready until receiving request
// //   chaAssume(this, "memReqRea |-> X((memReqRea U memReqFire) || G memReqRea)")

// //   // chaAssert(this, "F s2ReqFire")
// //   // chaAssume(this, "mem")
// //   val ns2ReqValid = !s2ReqValid
// //   chaAssert(this, "(ns2ReqValid ##1 s2ReqValid) |-> F s3ReqValid")
// //   // chaAssert(this, "hitReq |-> F s3ReqValid")
// //   // chaAssert(this, "(nhitReq && reqIsRead)|-> F s3ReqValid")
  
// // }

// // class CacheStage3Safe extends CacheStage3()(CacheConfig()){
// //   val isIdle = state === s_idle
// //   val isRelease = state === s_release
// //   val cond = probe && io.cohResp.fire() && hit 

// //   chaAssert(this, "isIdle |-> ##1 isRelease")
// // }

// // class CacheProp extends Cache()(CacheConfig()){
// //   val s1Valid = s1.io.out.valid && s2.io.in.ready
// //   // val s2Ready = 
// //   val s2Valid = s2.io.in.valid
// //   val nFlush = !io.flush(0)

// //   // chaAssume(this, "nFlush")
// //   // chaAssert(this, "s1Valid |-> ##1 s2Valid")

// //   val inReq = io.in.req.valid
// //   val outReq = s1.io.in.valid
// //   chaAssert(this, "inReq |-> ##[0:1] outReq")

// //   // val metaRead = s1.io.metaReadBus.req.valid
// //   // val metaReadReady = s1.io.metaReadBus.req.ready
// //   // val metaReadResp = s1.io.metaReadBus.resp.data(4).valid

// //   // chaAssert(this, "(metaRead && metaReadReady)  |-> ##1 metaReadResp")
// // }

// class CacheProp extends Cache()(CacheConfig()){
//   // val s1Valid = s1.io.out.valid && s2.io.in.ready
//   // // val s2Ready = 
//   // val s2Valid = s2.io.in.valid
//   // val nFlush = !io.flush(0)

//   // // chaAssume(this, "nFlush")
//   // // chaAssert(this, "s1Valid |-> ##1 s2Valid")

//   // val inReq = io.in.req.valid
//   // val outReq = io.in.resp.valid
//   // chaAssert(this, "inReq |-> F outReq")

//   // val metaRead = s1.io.metaReadBus.req.valid
//   // val metaReadReady = s1.io.metaReadBus.req.ready
//   // val metaReadResp = s1.io.metaReadBus.resp.data(4).valid


//   val p1 = s2.io.out.bits.hit
//   val p2 = s2.io.out.bits.mmio
  
//   assert(!p1 || !p2)
//   // chaAssert(this, "(metaRead && metaReadReady)  |-> ##1 metaReadResp")
// }

// class CachePropSpec extends AnyFlatSpec with ChiselScalatestTester with Formal {

//   // println(new (chisel3.stage.ChiselStage).emitSystemVerilog(new CacheStage1Prop()))
  
//   // behavior of "CacheStage1"
//   // it should "pass" in {
//   //   verify(new CacheStage1Prop, Seq(BoundedCheck(1), PonoEngineAnnotation))
//   // }

//   // behavior of "CacheStage2"
//   // it should "pass" in {
//   //   verify(new CacheStage2Prop()(new CacheConfig()), Seq(BoundedCheck(50), PonoEngineAnnotation))
//   // }

//   // behavior of "CacheStage3"
//   // it should "pass" in {
//   //   verify(new CacheStage3Prop()(new CacheConfig()), Seq(BoundedCheck(50), PonoEngineAnnotation, EnSafetyOpti))
//   //   // verify(new CacheStage3Prop, Seq(Ic3SaCheck(50), PonoEngineAnnotation))
//   // }

//   // behavior of "CacheStage3Safe"
//   // it should "pass" in {
//   //   verify(new CacheStage3Safe, Seq(BoundedCheck(200), PonoEngineAnnotation))
//   // }
  
//   // println(new (chisel3.stage.ChiselStage).emitSystemVerilog(new CacheProp))

//   // behavior of "Cache"
//   // it should "pass" in {
//   //   verify(new CacheProp, Seq(KInductionCheck(100), PonoEngineAnnotation))z
//   // }

//   behavior of "Cache"
//   it should "pass" in {
//     verify(new CacheArrayProp()(new CacheConfig()), Seq(KInductionCheck(100), PonoEngineAnnotation))
//   }
// }

// // class SRAM(implicit val cacheConfig: CacheConfig = CacheConfig()) extends CacheModule {
  
// //   val metaArray = Module(new SRAMTemplateWithArbiter(nRead = 1, new MetaBundle, set = Sets, way = Ways, shouldReset = true))

// //   val temp = metaArray.io.w.req.valid
  
// //   // to verify that request will always be responsed ultimately
// //   chaAssert(this,"temp")
// // }

// // class SRAMPropSpec extends AnyFlatSpec with ChiselScalatestTester with Formal {
// //   // println(new (chisel3.stage.ChiselStage).emitSystemVerilog(new CacheStage1Prop()))
// //   behavior of "SRAM"
// //   it should "pass" in {
// //     verify(new SRAM(), Seq(Ic3SaCheck(50), PonoEngineAnnotation))
// //   }
// // }