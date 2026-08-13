pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        timeout(time: 20, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {
        stage('Checkout Source') {
            steps {
                echo 'Đang thực hiện checkout mã nguồn từ Git...'
                checkout scm
            }
        }

        stage('Show Environment') {
            steps {
                sh '''
                    set -eux
                    java -version
                    mvn -version
                    git --version
                    pwd
                '''
            }
        }

        stage('Validate Project') {
            steps {
                script {
                    if (!fileExists('pom.xml')) {
                        error "LỖI: Không tìm thấy file 'pom.xml' ở thư mục gốc của dự án. Hủy pipeline!"
                    }
                }
            }
        }

        stage('Prepare Environment') {
            steps {
                script {
                    if (!fileExists('.env') && fileExists('.env.example')) {
                        echo 'Tự động sao chép .env.example -> .env cho môi trường CI...'
                        sh 'cp .env.example .env'
                    }
                }
            }
        }

        stage('Test') {
            steps {
                sh '''
                    set -eux
                    if [ -f "./mvnw" ]; then
                        echo "Phát hiện mvnw. Đang cấp quyền thực thi và chạy test..."
                        chmod +x ./mvnw
                        ./mvnw clean test
                    else
                        echo "Không phát hiện mvnw. Sử dụng Maven hệ thống để chạy test..."
                        mvn clean test
                    fi
                '''
            }
        }

        stage('Build') {
            steps {
                sh '''
                    set -eux
                    if [ -f "./mvnw" ]; then
                        echo "Đang đóng gói dự án sử dụng mvnw..."
                        chmod +x ./mvnw
                        ./mvnw clean package -DskipTests
                    else
                        echo "Đang đóng gói dự án sử dụng Maven hệ thống..."
                        mvn clean package -DskipTests
                    fi
                '''
            }
        }

        stage('Archive Artifact') {
            steps {
                echo 'Đang lưu trữ sản phẩm build (artifact)...'
                archiveArtifacts artifacts: 'target/*.jar', excludes: 'target/*original*.jar, target/*sources*.jar, target/*javadoc*.jar'
            }
        }
    }

    post {
        success {
            echo 'KẾT QUẢ: Pipeline chạy thành công tốt đẹp!'
        }
        unstable {
            echo 'KẾT QUẢ: Pipeline chạy hoàn thành nhưng có một số kiểm thử hoặc cảnh báo không ổn định (unstable)!'
        }
        failure {
            echo 'KẾT QUẢ: Pipeline thất bại! Vui lòng kiểm tra lại log chi tiết của các stage.'
        }
        always {
            echo "Trạng thái build hiện tại: ${currentBuild.currentResult}"
            echo 'Đang dọn dẹp thư mục làm việc (Workspace)...'
            cleanWs()
        }
    }
}
