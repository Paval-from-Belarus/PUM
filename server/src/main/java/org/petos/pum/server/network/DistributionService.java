package org.petos.pum.server.network;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.petos.pum.server.dto.PackageInfoMapper;
import org.petos.pum.server.repositories.PackageAliasDao;
import org.petos.pum.server.repositories.PackageHatDao;
import org.petos.pum.server.repositories.PackageInfoDao;
import org.petos.pum.server.repositories.PackageTypeDao;
import org.petos.pum.server.repositories.entities.PackageAlias;
import org.petos.pum.server.repositories.entities.PackageHat;
import org.petos.pum.server.repositories.entities.PackageInfo;
import org.petos.pum.networks.grpc.DistributionServiceGrpc;
import org.petos.pum.networks.grpc.FullInstanceInfo;
import org.petos.pum.networks.grpc.HeaderInfo;
import org.petos.pum.networks.grpc.HeaderRequest;
import org.petos.pum.networks.grpc.InstanceRequest;
import org.petos.pum.networks.grpc.PayloadChunk;
import org.petos.pum.networks.grpc.PayloadRequest;
import org.petos.pum.server.dto.PackageHatMapper;

import java.util.Optional;

/**
 * @author Paval Shlyk
 * @since 16/09/2023
 */
@GrpcService
@RequiredArgsConstructor
@Deprecated
public class DistributionService extends DistributionServiceGrpc.DistributionServiceImplBase {
private final PackageTypeDao typeDao;
private final PackageInfoDao infoDao;
private final PackageAliasDao aliasDao;
private final PackageHatDao hatDao;
private final PackageHatMapper hatMapper;
private final PackageInfoMapper infoMapper;

//this method should return common info about packages
@Override
public void onHeaderRequest(HeaderRequest request, StreamObserver<HeaderInfo> responseObserver) {
      Optional<PackageAlias> hatAlias = aliasDao.findByName(request.getPackageAlias());
      if (hatAlias.isPresent()) {
	    PackageHat hat = hatAlias.get().getHat();
	    HeaderInfo header = hatMapper.toHeaderInfo(hat);
	    responseObserver.onNext(header);
	    responseObserver.onCompleted();
      } else {
	    String errorMessage = String.format("package header %s doesn't exist", request.getPackageAlias());
	    responseObserver.onError(Status.NOT_FOUND.withDescription(errorMessage).asException());
      }
}

@Override
public void onInstanceRequest(InstanceRequest request, StreamObserver<FullInstanceInfo> responseObserver) {
      Optional<PackageInfo> info = infoDao.findByPackageIdAndVersion(request.getPackageId(), request.getVersion());
      if (info.isPresent()) {
	    FullInstanceInfo instance = infoMapper.toFullInfo(info.get());
	    responseObserver.onNext(instance);
	    responseObserver.onCompleted();
      }
}

@Override
public void onPayloadRequest(PayloadRequest request, StreamObserver<PayloadChunk> responseObserver) {
      super.onPayloadRequest(request, responseObserver);
}
}
